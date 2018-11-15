package fssi.edgenode.handler
import fssi.types.ApplicationMessage
import fssi.types.biz.Message

trait ApplicationMessageHandler {

  private val applicationMessageHandler = Message.handler[ApplicationMessage] { message =>
    }

  def apply(): Message.Handler[ApplicationMessage] = applicationMessageHandler
}
