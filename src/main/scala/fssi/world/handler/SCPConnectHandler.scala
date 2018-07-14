package fssi.world.handler

import bigknife.scalap.ast.types._
import bigknife.scalap.world.Connect
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.BytesValue
import fssi.ast.domain.types.DataPacket.TypedString
import fssi.ast.usecase.Warrior
import fssi.interpreter.{Setting, runner}
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import fssi.interpreter.scp.MomentValue
import bigknife.scalap.interpreter.{runner => scprunner}
import bigknife.scalap.ast.usecase.{SCP, component => scpcomp}

class SCPConnectHandler(setting: Setting, warrior: Warrior[Model.Op]) extends Connect {
  override def extractValidValue(value: Value): Option[Value] = None

  override def validateValue(value: Value): Value.Validity = {
    //todo validate more
    Value.Validity.FullyValidated
  }

  override def signData(bytes: Array[Byte], nodeID: NodeID): Signature = {
    val data =
      runner.runIO(warrior.signData(bytes, BytesValue(nodeID.bytes)), setting).unsafeRunSync()
    Signature(data.bytes)
  }

  override def broadcastMessage[M <: Message](envelope: Envelope[M]): Unit = {
    val json = envelope.asJson
    val msg  = TypedString(json.noSpaces, "scp.envelope")
    val p    = warrior.broadcastMessage(msg)
    runner.runIO(p, setting).unsafeRunSync()
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
    runner.runIO(p, setting).unsafeRunSync()
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
    scprunner.runIO(p, setting.toScalapSetting(nodeID)).unsafeRunSync()
  }

  override def valueExternalized(nodeID: NodeID, slotIndex: SlotIndex, value: Value): Unit = {
    // value externalized, block determined
    runner
      .runIO(warrior.momentsDetermined(value.asInstanceOf[MomentValue].moments, slotIndex.index),
             setting)
      .unsafeRunSync()
  }
}

object SCPConnectHandler {
  val scp: SCP[scpcomp.Model.Op] = SCP[scpcomp.Model.Op]
}
