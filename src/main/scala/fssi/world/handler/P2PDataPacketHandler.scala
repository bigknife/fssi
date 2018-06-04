package fssi.world.handler

import fssi.ast.domain.components.Model
import fssi.ast.domain.types.DataPacket
import fssi.ast.domain.types.DataPacket.CreateAccount
import fssi.ast.usecase.Warrior
import org.slf4j.{Logger, LoggerFactory}

trait P2PDataPacketHandler {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def handle(dataPacket: DataPacket, warrior: Warrior[Model.Op]): Unit = dataPacket match {
    case CreateAccount(account) =>
      logger.info(s"This is a message from Nymph: $account")

  }
}
