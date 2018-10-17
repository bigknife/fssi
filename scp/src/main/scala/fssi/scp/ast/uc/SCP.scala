package fssi.scp
package ast
package uc

import bigknife.sop._

import types._
import components._

trait SCP[F[_]] {

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID, slotIndex: SlotIndex, value: Value): SP[F, Boolean]

  /** process message envelope from peer nodes
    */
  def handleSCPEnvelope(nodeId: NodeID, slotIndex: SlotIndex, envelope: Envelope): SP[F, Boolean]
}

object SCP {
  def apply[F[_]](implicit M: Model[F]): SCP[F] =
    new SCP[F] with HandleRequestProgram[F] with HandleSCPEnvelopeProgram[F] {
      private[uc] val model: Model[F] = M
    }
}
