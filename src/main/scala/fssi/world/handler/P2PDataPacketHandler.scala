package fssi.world.handler


import bigknife.scalap.ast.types.{Envelope, Message, NodeID}
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

      case SubmitTransaction(account, transaction) =>
        val setting =
          warriorArgs.toSetting.copy(scpConnect = new SCPConnectHandler(warriorArgs.toSetting, warrior))
        runner
          .runIOAttempt(warrior.processTransaction(transaction), setting)
          .unsafeRunSync() match {
          case Left(t)       => logger.error(s"failed to process transaction(${transaction.id})", t)
          case Right(status) => logger.info(s"processing transaction($status)")
        }

      case DataPacket.ScpEnvelope(message) =>
        //todo message to Envelope, then invoke scp process envelope to handle it.
        parse(message) match {
          case Left(t) => logger.error(s"received bad SCPEnvelope message: $message")
          case Right(json) =>
            val node = runner.runIO(warrior.currentNode(), warriorArgs.toSetting).unsafeRunSync()
            val nodeID = NodeID(node.boundAccount.get.publicKeyData.bytes)

            json.as[Envelope[Message]] match {
              case Left(t) => logger.error(s"SCPEnvelope message deserialize failed: $message")
              case Right(envelope) =>
                val p = scp.processEnvelope(nodeID, envelope)
                val state = scprunner.runIO(p, warriorArgs.toSetting.toScalapSetting(nodeID)).unsafeRunSync()
                logger.info(s"scp processed envelope, state is $state")
            }



        }


      case x => logger.warn(s"unsupported messages: $x")

    }
}
