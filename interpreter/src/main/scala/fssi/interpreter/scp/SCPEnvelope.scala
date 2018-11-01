package fssi.interpreter.scp

import fssi.scp.types._
import fssi.types.biz.ConsensusAuxMessage

case class SCPEnvelope(value: Envelope[Message]) extends ConsensusAuxMessage
