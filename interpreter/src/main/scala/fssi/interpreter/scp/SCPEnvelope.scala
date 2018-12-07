package fssi.interpreter.scp

import fssi.scp.types._
import fssi.types.biz.Message.ConsensusMessage

case class SCPEnvelope(value: Envelope[Message]) extends ConsensusMessage {

  type NM = fssi.scp.types.Message.Nomination
  type PM = fssi.scp.types.Message.Prepare
  type CM = fssi.scp.types.Message.Confirm
  type EM = fssi.scp.types.Message.Externalize

  def messageType: String = {
    value.statement.message match {
      case _: PM => "prepare"
      case _: CM => "confirm"
      case _: EM => "externalize"
      case _: NM => "nom"
    }
  }

  def slotIndex: BigInt = value.statement.slotIndex.value
}
