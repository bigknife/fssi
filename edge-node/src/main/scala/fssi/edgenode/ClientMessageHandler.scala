package fssi.edgenode
import fssi.types.ClientMessage
import fssi.types.biz.Message

trait ClientMessageHandler {
  private val handler = Message.handler[ClientMessage] { message =>
    }

  def apply(): ClientMessageHandler = handler
}
