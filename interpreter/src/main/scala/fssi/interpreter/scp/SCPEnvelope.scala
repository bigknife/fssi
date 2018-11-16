package fssi.interpreter.scp

import fssi.scp.types._
import fssi.types.biz.Message.ConsensusMessage

case class SCPEnvelope(value: Envelope[Message]) extends ConsensusMessage
