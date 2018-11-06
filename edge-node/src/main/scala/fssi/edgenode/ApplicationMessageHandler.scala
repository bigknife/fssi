package fssi.edgenode
import fssi.types.ApplicationMessage
import fssi.types.biz.Message

trait ApplicationMessageHandler {
  private val handler = Message.handler[ApplicationMessage] { message =>
    }

  def apply(): Message.Handler[ApplicationMessage] = handler
}
