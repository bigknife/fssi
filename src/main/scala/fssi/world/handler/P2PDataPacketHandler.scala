package fssi.world.handler


import java.nio.charset.Charset

import bigknife.scalap.ast.types.{Envelope, Message, NodeID, QuorumSet}
import bigknife.scalap.ast.usecase.SCP
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.DataPacket
import fssi.ast.domain.types.DataPacket.{CreateAccount, SubmitTransaction, SyncAccount, TypedString}
import fssi.ast.usecase.Warrior
import fssi.interpreter.runner
import fssi.world.Args.WarriorArgs
import org.slf4j.{Logger, LoggerFactory}
import io.circe.parser._
import io.circe.syntax._
import bigknife.scalap.ast.usecase.{component => scpcomp}
import bigknife.scalap.interpreter.{runner => scprunner}
import fssi.interpreter.jsonCodec._
import fssi.interpreter.scp.{QuorumSetSync, SCPDataPacket, SCPExecutionService}
import io.circe

trait P2PDataPacketHandler {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val scp: SCP[scpcomp.Model.Op] = SCP[scpcomp.Model.Op]

  def handle(dataPacket: DataPacket, warrior: Warrior[Model.Op], warriorArgs: WarriorArgs): Unit =
    dataPacket match {
      case CreateAccount(account) =>
        runner
          .runIOAttempt(warrior.createNewAccount(account), warriorArgs.toSetting)
          .unsafeRunSync() match {
          case Left(t)  => logger.error(s"handle create account($account) message failed", t)
          case Right(_) => logger.info(s"handle create account($account) message finished")
        }

      case SyncAccount(id) =>
        //todo SyncAccount
        logger.warn("this feature is been developed")

      case SubmitTransaction(transaction) =>
        SCPExecutionService.submit {
          val setting =
            warriorArgs.toSetting.copy(scpConnect = new SCPConnectHandler(warriorArgs.toSetting, warrior))
          runner
            .runIOAttempt(warrior.processTransaction(transaction), setting)
            .unsafeRunSync() match {
            case Left(t)       => logger.error(s"failed to process transaction(${transaction.id})", t)
            case Right(status) => logger.info(s"processing transaction($status)")
          }
        }


      case SCPDataPacket.ScpEnvelope(envelope) =>
        val node = runner.runIO(warrior.currentNode(), warriorArgs.toSetting).unsafeRunSync()
        val nodeID = NodeID(node.accountPublicKey.bytes)
        logger.info(s"got currentNode: $nodeID")
        SCPExecutionService.submit {
          val setting =
            warriorArgs.toSetting.copy(scpConnect = new SCPConnectHandler(warriorArgs.toSetting, warrior))
          val p = scp.processEnvelope(nodeID, envelope)
          val state = scprunner.runIO(p, setting.toScalapSetting(nodeID)).unsafeRunSync()
          logger.info(s"scp processed envelope, state is $state")
        }

      case SCPDataPacket.QuorumSetSyncMsg(qss) =>
        val setting =
          warriorArgs.toSetting.copy(scpConnect = new SCPConnectHandler(warriorArgs.toSetting, warrior))

        val node = runner.runIO(warrior.currentNode(), setting).unsafeRunSync()
        val nodeID = NodeID(node.accountPublicKey.bytes)


        val f = better.files.File(setting.workFileOfName(SCPDataPacket.QuorumSetJsonFile))
        f.createIfNotExists()
        val qssLocal =  {
          for {
            json <- parse(f.contentAsString(Charset.forName("utf-8")))
            qss <- json.as[QuorumSetSync]
          } yield qss
        }.toOption.getOrElse({
          val hashCurrent = QuorumSetSync.hash(0L, setting.scpRegisteredQuorumSets)
          QuorumSetSync(0, setting.scpRegisteredQuorumSets, hashCurrent)
        })

        if (qssLocal.isSane) {
          val newQss = qssLocal.merge(qss)

          if (newQss != qssLocal || newQss != qss) {
            // save to local
            // broadcast newQss
            val newQssJson = newQss.asJson
            f.overwrite(newQssJson.spaces2)

            val p1 = warrior.broadcastMessage(SCPDataPacket.syncQuorumSets(newQss))
            runner.runIO(p1, setting).unsafeRunSync()
            logger.info("local quorumsets updated, have broadcast the new QuorumSetSync")

            SCPExecutionService.submit {
              val node = runner.runIO(warrior.currentNode(), warriorArgs.toSetting).unsafeRunSync()
              val nodeID = NodeID(node.accountPublicKey.bytes)
              val p2 = scp.quorumSetsUpdated(newQss.registeredQuorumSets)
              scprunner.runIO(p2, setting.toScalapSetting(nodeID)).unsafeRunSync()
              logger.info("scp handled quorumsets updated")
            }

          }
        } else {
          logger.error("received quorumset message is not SANE")
        }


      case x => logger.warn(s"unsupported messages: $x")

    }
}
