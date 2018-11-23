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
    *
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
        leaders <- updateAndGetNominateLeaders(slotIndex, previousValue)
        votes <- ifM(leaders.contains(nodeId), ValueSet(value)) {
          leaders.foldLeft(ValueSet.empty.pureSP[F]) { (acc, n) =>
            for {
              pre      <- acc
              envelope <- getNominationEnvelope(slotIndex, n)
              next <- ifM(envelope.isEmpty, pre) {
                for {
                  v <- tryGetNewValueFromNomination(nodeId,
                                                    slotIndex,
                                                    previousValue,
                                                    envelope.get.statement.message,
                                                    round)
                } yield if (v.isEmpty) pre else pre + v.get
              }

            } yield next
          }
        }
      } yield votes
    }

    ifM(cannotNominateNewValue(slotIndex, timeout), false.pureSP[F]) {
      // rate limits
      for {
        _        <- startNominating(slotIndex)
        round    <- currentNominateRound(slotIndex)
        _        <- debug(s"[$nodeId][$slotIndex] handling app request at round: $round")
        votes    <- narrowDownVotes(round)
        newVotes <- filtrateVotes(votes)
        _        <- debug(s"[$nodeId][$slotIndex] narrowdown votes: $newVotes")
        voted <- ifM(newVotes.isEmpty, right = false) {
          for {
            _       <- voteNewNominations(slotIndex, newVotes)
            _       <- debug(s"[$nodeId][$slotIndex] vote new nomination: $newVotes")
            message <- createNominationMessage(slotIndex)
//            envelope <- putInEnvelope(slotIndex, message)
//            handled  <- handleSCPEnvelope(envelope, previousValue)
//            _        <- debug(s"[$nodeId][$slotIndex] handle nomination envelope locally: $handled")
//            _ <- ifThen(handled) {
//              for {
//                _ <- emitNomination(slotIndex, previousValue, message)
//              } yield ()
//
//            }
//          } yield handled
            _ <- emitNomination(slotIndex, previousValue, message)
          } yield true
        }

        timeout <- computeTimeout(round)
        _       <- gotoNextNominateRound(slotIndex)
        _       <- debug(s"[$nodeId][$slotIndex] has gone to next round")
        _ <- delayExecuteProgram(NOMINATE_TIMER,
                                 handleAppRequest(nodeId, slotIndex, value, previousValue),
                                 timeout)
        _ <- debug(s"[$nodeId][$slotIndex] delay execute handleAppRequest after $timeout")
      } yield voted
    }
  }

  private[uc] def tryGetNewValueFromNomination(nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               previousValue: Value,
                                               nom: Message.Nomination,
                                               round: Int): SP[F, Option[Value]]
}
