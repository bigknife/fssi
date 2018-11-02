package fssi.scp
package ast
package uc

import bigknife.sop._

import types._
import components._

trait SCP[F[_]] {

  /** initialize node scp
    */
  def initialize(): SP[F, Unit]

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

  /** a bridge function, nomination process can bump to ballot process
    */
  private[uc] def bumpState(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            compositeValue: Value,
                            force: Boolean): SP[F, Boolean]
}

object SCP {
  def apply[F[_]](implicit M: Model[F]): SCP[F] =
    new HandleAppRequestProgram[F] with HandleSCPEnvelopeProgram[F] with HandleNominationProgram[F]
    with HandleBallotMessageProgram[F] with BumpStateProgram[F] with AttemptAcceptPrepareProgram[F]
    with AttemptConfirmPrepareProgram[F] with AttemptAcceptCommitProgram[F]
    with AttemptConfirmCommitProgram[F] with InitializeProgram[F] {
      private[uc] val model: Model[F] = M
    }
}
