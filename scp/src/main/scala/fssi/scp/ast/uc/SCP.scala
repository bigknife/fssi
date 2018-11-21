package fssi.scp
package ast
package uc

import bigknife.sop._

import types._
import components._

trait SCP[F[_]] {

  /** initialize node scp
    */
  def initialize(nodeId: NodeID,
                 quorumSet: QuorumSet,
                 currentHeight: BigInt,
                 fakeValue: Value): SP[F, Unit]

  /** nominate fake value to update round leaders
    */
  def nominateFakeValue(nodeId: NodeID, slotIndex: SlotIndex, fakeValue: Value): SP[F, Unit]

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value): SP[F, Boolean]

  /** process message envelope from peer nodes
    */
  def handleSCPEnvelope[M <: Message](envelope: Envelope[M], previousValue: Value): SP[F, Boolean]

  /** a bridge function, nomination process can bump to ballot process
    */
  private[scp] def bumpState(slotIndex: SlotIndex,
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
