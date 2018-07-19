package fssi.world.handler

import java.nio.charset.Charset

import bigknife.scalap.ast.types._
import bigknife.scalap.world.Connect
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.{BytesValue, DataPacket, Moment}
import fssi.ast.domain.types.DataPacket.TypedString
import fssi.ast.usecase.Warrior
import fssi.interpreter.{CryptoServiceHandler, Setting, runner}
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import fssi.interpreter.scp.{MomentValue, QuorumSetSync, SCPDataPacket, SCPExecutionService}
import bigknife.scalap.interpreter.{runner => scprunner}
import bigknife.scalap.ast.usecase.{SCP, component => scpcomp}
import fssi.contract.States
import org.slf4j.LoggerFactory

class SCPConnectHandler(setting: Setting, warrior: Warrior[Model.Op]) extends Connect {
  private val logger = LoggerFactory.getLogger(getClass)

  private lazy val innerSetting: Setting = setting.copy(scpConnect = this)

  override def synchronizeQuorumSet(nodeID: NodeID, quorumSet: QuorumSet): Unit = {
    // load from local qs file,
    // if found, merge and broadcast
    // todo: design a protocol to sync qs
    import io.circe.parser._
    import fssi.interpreter.jsonCodec._

    val f = better.files.File(innerSetting.workFileOfName(SCPDataPacket.QuorumSetJsonFile))
    f.createIfNotExists()

    val ret = for {
      json <- parse(f.contentAsString(Charset.forName("utf-8")))
      qss  <- json.as[QuorumSetSync]
    } yield {
      val version =
        if (!qss.registeredQuorumSets.contains(nodeID) ||
            qss.registeredQuorumSets(nodeID) != quorumSet) qss.version + 1L
        else qss.version
      val qs   = qss.registeredQuorumSets + (nodeID -> quorumSet)
      val hash = QuorumSetSync.hash(version, qs)
      QuorumSetSync(version, qs, hash)
    }

    val qssNew = ret.toOption.getOrElse({
      val version = 0L
      val qs      = Map(nodeID -> quorumSet)
      val hash    = QuorumSetSync.hash(version, qs)
      QuorumSetSync(version, qs, hash)
    })

    val dataPacket = SCPDataPacket.syncQuorumSets(qssNew)

    // delay 5seconds wait for some peers
    fssi.interpreter.util.delay(5000L) {
      val p = warrior.broadcastMessage(dataPacket)
      runner.runIO(p, innerSetting).unsafeRunSync()
    }
  }

  override def extractValidValue(value: Value): Option[Value] = None

  override def validateValue(value: Value): Value.Validity = {
    //todo: maybe partially valid makes sense.
    value match {
      case MomentValue(moments) =>
        // last moments' states jump should update to next moment to prevent system from double-spend attacking
        def validateMoment(m: Moment, lastStates: States): (Boolean, States) = {
          // timestamp should < current
          val timestampIsLegal = m.timestamp < System.currentTimeMillis()

          // validate state hash
          val oldStatesHash = CryptoServiceHandler.implicits.cryptoServiceHandler
            .hash(BytesValue(m.oldStates.bytes))(innerSetting)
            .unsafeRunSync()
          val newStatesHash = CryptoServiceHandler.implicits.cryptoServiceHandler
            .hash(BytesValue(m.newStates.bytes))(innerSetting)
            .unsafeRunSync()
          val hashIsValid = m.oldStatesHash == oldStatesHash && m.newStatesHash == newStatesHash

          // validate oldState
          // when run transaction, pass the last states to it. it' will update the latest
          // states of the account(relative), to prevent double-spend attack
          val tmpMoment =
            runner
              .runIO(warrior.runTransaction(m.transaction, lastStates), innerSetting)
              .unsafeRunSync()
          val tempHashIsValid = tmpMoment.exists(x =>
            x.oldStatesHash == m.oldStatesHash && x.newStatesHash == m.newStatesHash)

          (timestampIsLegal && hashIsValid && tempHashIsValid,
           tmpMoment.map(_.newStates).getOrElse(States.empty))
        }

        moments.foldLeft((true, States.empty)) { (acc, n) =>
          if (!acc._1) acc
          else validateMoment(n, acc._2)
        } match {
          case (false, _) =>
            Value.Validity.invalid
          case _ =>
            Value.Validity.fullyValidated
        }
      case _ => Value.Validity.invalid
    }
  }

  override def signData(bytes: Array[Byte], nodeID: NodeID): Signature = {
    val data =
      runner.runIO(warrior.signData(bytes, BytesValue(nodeID.bytes)), innerSetting).unsafeRunSync()
    Signature(data.bytes)
  }

  override def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = {
    val msg = SCPDataPacket.scpEnvelope(envelope)
    val p   = warrior.broadcastMessage(msg)
    runner.runIO(p, innerSetting).unsafeRunSync()
  }

  override def verifySignature[M <: Message](envelope: Envelope[M]): Boolean = {
    import bigknife.scalap.ast.types.implicits._
    val source: Array[Byte] = envelope.statement match {
      case x: Statement.Nominate    => x.bytes
      case x: Statement.Prepare     => x.bytes
      case x: Statement.Commit      => x.bytes
      case x: Statement.Externalize => x.bytes
    }
    val p = warrior.verifySign(source, envelope.signature.bytes, envelope.statement.nodeID.bytes)
    runner.runIO(p, innerSetting).unsafeRunSync()
  }

  override def combineValues(valueSet: ValueSet): Value = {
    val mv = valueSet.foldLeft(MomentValue.empty) { (acc, n) =>
      n match {
        case x: MomentValue => MomentValue(acc.moments ++ x.moments)
        case _              => acc
      }
    }
    // sort
    MomentValue(mv.moments.sortBy(_.timestamp))
  }

  override def runAbandonBallot(nodeID: NodeID, slotIndex: SlotIndex, counter: Int): Unit = {
    import SCPConnectHandler._
    val p = scp.abandonBallot(nodeID, slotIndex, counter)
    scprunner.runIO(p, innerSetting.toScalapSetting(nodeID)).unsafeRunSync()
  }

  override def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
    // value externalized, block determined
    runner
      .runIO(warrior.momentsDetermined(value.asInstanceOf[MomentValue].moments, slotIndex.index),
             innerSetting)
      .unsafeRunSync()
  }

  override def timeoutForNextRoundNominating(currentRound: Int): Long = {
    // the first ten rounds run every 1seconds, and then linear increasing
    if (currentRound > 10) (currentRound - 10) * 1000L
    else 1000L
  }

  override def triggerNextRoundNominating(nodeID: NodeID,
                                          slotIndex: SlotIndex,
                                          nextRound: Int,
                                          valueToNominate: Value,
                                          previousValue: Value,
                                          afterMilliSeconds: Long): Unit = {
    import SCPConnectHandler._
    val timer = new java.util.Timer()
    timer.schedule(
      new java.util.TimerTask {
        override def run(): Unit = {
          logger.info("run nominate timer ...")
          // only re-nominate when slotIndex is currentHeight + 1
          val currentHeight = runner.runIO(warrior.currentHeight(), innerSetting).unsafeRunSync()
          if (slotIndex.index == (currentHeight + 1)) {
            SCPExecutionService.submit {
              val p   = scp.nominate(nodeID, slotIndex, nextRound, valueToNominate, previousValue)
              val ret = scprunner.runIO(p, innerSetting.toScalapSetting(nodeID)).unsafeRunSync()
              logger.info(s"nominate result: $ret")
            }
            logger.info("end and cancel nominate timer ...")
            timer.cancel()
          } else {
            logger.info(s"slotIndex[${slotIndex.index}] has been determined, timer cancel!")
            timer.cancel()
          }
        }
      },
      afterMilliSeconds
    )

  }
}

object SCPConnectHandler {
  val scp: SCP[scpcomp.Model.Op] = SCP[scpcomp.Model.Op]
}
