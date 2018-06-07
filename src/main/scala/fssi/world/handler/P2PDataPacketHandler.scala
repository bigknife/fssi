package fssi.world.handler

import fssi.ast.domain.components.Model
import fssi.ast.domain.types.DataPacket
import fssi.ast.domain.types.DataPacket.{CreateAccount, SubmitTransaction, SyncAccount}
import fssi.ast.usecase.Warrior
import fssi.interpreter.runner
import fssi.world.Args.WarriorArgs
import org.slf4j.{Logger, LoggerFactory}

trait P2PDataPacketHandler {
  val logger: Logger = LoggerFactory.getLogger(getClass)

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
        runner
          .runIOAttempt(warrior.processTransaction(transaction), warriorArgs.toSetting)
          .unsafeRunSync() match {
          case Left(t)       => logger.error(s"failed to process transaction(${transaction.id})", t)
          case Right(status) => logger.info(s"processing transaction($status)")
        }

    }
}
