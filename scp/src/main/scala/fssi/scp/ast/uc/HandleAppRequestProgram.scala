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
    handleAppRequest(nodeId, slotIndex, value, previousValue, timeout = false)
  }

  /** handle request of application
    * @param timeout when re-nominate after a while,
    *                this timeout should be true, for a app request, it should always be false
    */
  private def handleAppRequest(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       value: Value,
                       previousValue: Value,
                       timeout: Boolean): SP[F, Boolean] = {

    def narrowDownVotes(round: Int): SP[F, ValueSet] = {
      for {
        leaders <- updateAndGetNominateLeaders(nodeId, slotIndex, previousValue)
        votes <- ifM(leaders.contains(nodeId), ValueSet(value)) {
          leaders.foldLeft(ValueSet.empty.pureSP[F]) {(acc, n) =>
            for {
              pre <- acc
              envelope <- getNominationEnvelope(nodeId, slotIndex, n)
              next <- ifM(envelope.isEmpty, pre) {
                for {
                  v <- tryGetNewValueFromNomination(nodeId, slotIndex, previousValue, envelope.get.statement.message, round)
                } yield if(v.isEmpty) pre else pre + v.get
              }

            } yield next
          }
        }
      } yield votes
    }

    ifM(cannotNominateNewValue(nodeId, slotIndex, timeout), false.pureSP[F]) {
      // rate limits
      for {
        round    <- currentNominateRound(nodeId, slotIndex)
        _        <- info(s"[$nodeId][$slotIndex] handling app request at round: $round")
        newVotes <- narrowDownVotes(round)
        _        <- debug(s"[$nodeId][$slotIndex] narrowdown votes: $newVotes")
        voted <- ifM(newVotes.isEmpty, right = false) {
          for {
            _        <- voteNewNominations(nodeId, slotIndex, newVotes)
            _        <- info(s"[$nodeId][$slotIndex] vote new nomination: $newVotes")
            message  <- createNominationMessage(nodeId, slotIndex)
            envelope <- putInEnvelope(nodeId, slotIndex, message)
            handled  <- handleSCPEnvelope(envelope, previousValue)
            _        <- info(s"[$nodeId][$slotIndex] handle nomination envelope locally: $handled")
            _ <- ifThen(handled) {
              for {
                _ <- broadcastEnvelope(nodeId, slotIndex, envelope)
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


  private[uc] def tryGetNewValueFromNomination(nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               previousValue: Value,
                                               nom: Message.Nomination,
                                               round: Int): SP[F, Option[Value]]
}
