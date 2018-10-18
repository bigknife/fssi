package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleRequestProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model._

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value): SP[F, Boolean] = {
    import nodeStore._
    import nodeService._

    ifM(cannotNominateNewValue(nodeId, slotIndex), false) {
      // rate limits
      for {
        newVotes <- narrowDownVotes(nodeId, slotIndex, ValueSet(value), previousValue)
        voted <- ifM(newVotes.isEmpty, false) {
          for {
            _        <- voteNewNominations(nodeId, slotIndex, newVotes)
            message  <- createVoteNominationMessage(nodeId, slotIndex)
            envelope <- putInEnvelope(nodeId, message)
            handled  <- handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)
            _        <- ifThen(handled)(broadcastEnvelope(nodeId, envelope))
          } yield handled
        }
      } yield voted
    }
  }
}
