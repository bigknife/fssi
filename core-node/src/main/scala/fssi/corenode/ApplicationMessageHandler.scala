package fssi.corenode

import fssi.types.biz.Message

trait ApplicationMessageHandler {
  private val handler = Message.handler[Message.ApplicationMessage] {msg =>

  }

  def apply(): Message.Handler[Message.ApplicationMessage] = handler
}
