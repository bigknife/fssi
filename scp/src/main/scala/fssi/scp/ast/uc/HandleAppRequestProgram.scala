package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAppRequestProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._
  import model.logService._

  /** handle request of application
    */
  def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value): SP[F, Boolean] = {

    ifM(cannotNominateNewValue(nodeId, slotIndex), false.pureSP[F]) {
      // rate limits
      for {
        round    <- currentNominateRound(nodeId, slotIndex)
        _        <- info(s"[$nodeId][$slotIndex] handling app request at round: $round")
        newVotes <- narrowDownVotes(nodeId, slotIndex, ValueSet(value), previousValue)
        _        <- debug(s"[$nodeId][$slotIndex] narrowdown votes: $newVotes")
        voted <- ifM(newVotes.isEmpty, right = false) {
          for {
            _        <- voteNewNominations(nodeId, slotIndex, newVotes)
            _        <- info(s"[$nodeId][$slotIndex] vote new nomination: $newVotes")
            message  <- createNominationMessage(nodeId, slotIndex)
            envelope <- putInEnvelope(nodeId, message)
            handled  <- handleSCPEnvelope(nodeId, slotIndex, envelope, previousValue)
            _        <- info(s"[$nodeId][$slotIndex] handle nomination envelope locally: $handled")
            _ <- ifThen(handled) {
              for {
                _ <- broadcastEnvelope(nodeId, envelope)
                _ <- info(s"[$nodeId][$slotIndex] boradcast nomination envelope to peers")
              } yield ()

            }
          } yield handled
        }

        timeout <- computeTimeout(round)
        _       <- gotoNextNominateRound(nodeId, slotIndex)
        _       <- info(s"[$nodeId][$slotIndex] has gone to next round")
        _ <- delayExecuteProgram(NOMINATE_TIMER,
                                 handleAppRequest(nodeId, slotIndex, value, previousValue),
                                 timeout)
        _ <- info(s"[$nodeId][$slotIndex] delay execute handleAppRequest after $timeout")
      } yield voted
    }
  }
}
