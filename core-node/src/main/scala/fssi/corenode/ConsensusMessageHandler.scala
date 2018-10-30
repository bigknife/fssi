package fssi.corenode

import fssi.types.biz.Message
import fssi.types.biz.Message.ConsensusMessage

trait ConsensusMessageHandler {
  private val handler = Message.handler[ConsensusMessage] { msg =>
    }

  def apply(): Message.Handler[ConsensusMessage] = handler
}
