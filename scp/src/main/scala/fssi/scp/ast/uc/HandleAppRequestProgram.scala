package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAppRequestProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value): SP[F, Boolean] = {

    ifM(cannotNominateNewValue(nodeId, slotIndex), false.pureSP[F]) {
      // rate limits
      for {
        newVotes <- narrowDownVotes(nodeId, slotIndex, ValueSet(value), previousValue)
        voted <- ifM(newVotes.isEmpty, right = false) {
          for {
            _        <- voteNewNominations(nodeId, slotIndex, newVotes)
            message  <- createNominationMessage(nodeId, slotIndex)
            envelope <- putInEnvelope(nodeId, message)
            handled  <- handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)
            _        <- ifThen(handled)(broadcastEnvelope(nodeId, envelope))
          } yield handled
        }
        round   <- currentNominateRound(nodeId, slotIndex)
        timeout <- computeTimeout(round)
        _       <- gotoNextNominateRound(nodeId, slotIndex)
        _       <- delayExecuteProgram(NOMINATE_TIMER, handleAppRequest(nodeId, slotIndex, value, previousValue), timeout)
      } yield voted
    }
  }
}
