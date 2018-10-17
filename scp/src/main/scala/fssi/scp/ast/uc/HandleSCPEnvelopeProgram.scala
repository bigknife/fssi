package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleSCPEnvelopeProgram[F[_]] extends BaseProgram[F] {
  /** process message envelope from peer nodes
    */
  def handleSCPEnvelope(nodeId: NodeID, slotIndex: SlotIndex, envelope: Envelope): SP[F, Boolean] = {
    ???
  }
}
