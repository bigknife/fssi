package fssi.scp
package ast
package uc

import bigknife.sop._

import types._
import components._

trait SCP[F[_]] {

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value): SP[F, Boolean]

  /** process message envelope from peer nodes
    */
  def handleSCPEnvelope[M <: Message](nodeId: NodeID,
                                      slotIndex: SlotIndex,
                                      envelope: Envelope[M],
                                      previousValue: Value): SP[F, Boolean]
}

object SCP {
  def apply[F[_]](implicit M: Model[F]): SCP[F] =
    new HandleAppRequestProgram[F] with HandleSCPEnvelopeProgram[F] {
      private[uc] val model: Model[F] = M
    }
}
