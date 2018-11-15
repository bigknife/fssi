package fssi.edgenode.handler
import fssi.types.ClientMessage
import fssi.types.biz.Message

trait ClientMessageHandler {

  private val clientMessageHandler = Message.handler[ClientMessage] { message =>
    }

  def apply(): Message.Handler[ClientMessage] = clientMessageHandler
}
