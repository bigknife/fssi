package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleBallotMessageProgram[F[_]] extends SCP[F] with BumpStateProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def handleBallotMessage(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          envelope: Envelope[Message.BallotMessage]): SP[F, Boolean] = {

    //
    val statement = envelope.statement
    val message   = statement.message
    for {
      values   <- valuesFromBallotMessage(statement.message)
      validity <- validateValues(nodeId, slotIndex, values)
      r <- ifM(validity == Value.Validity.Invalid, false) {
        for {
          phase  <- currentBallotPhase(nodeId, slotIndex)
          commit <- currentCommitBallot(nodeId, slotIndex)
          invalidEnvelope = phase == Ballot.Phase.Externalize && commit.isDefined && commit.get.value != message.workingBallot.value
          _ <- ifThen(invalidEnvelope)(removeEnvelope(nodeId, slotIndex, envelope))
          _ <- ifThen(phase != Ballot.Phase.Externalize)(
            advanceSlot(nodeId, slotIndex, previousValue, statement))
        } yield !invalidEnvelope
      }
    } yield r
  }

  def advanceSlot(nodeId: NodeID,
                  slotIndex: SlotIndex,
                  previousValue: Value,
                  statement: Statement[Message.BallotMessage]): SP[F, Unit] = {

    //attemp bump to the latest un-accepted ballot
    // we can got a set of counters from envelopes: [3,5,6,7,9,10]
    // find a smallest,unaccepted counter, but is greater than currentBallot's counter.
    def isCounterAccepted(n: Int): SP[F, Boolean] = {
      for {
        aheadNodes <- nodesAheadBallotCounter(nodeId, slotIndex, n)
        q          <- isQuorum(nodeId, aheadNodes)
      } yield q
    }
    def isCounterNotAccepted(n: Int): SP[F, Boolean] = isCounterAccepted(n).map(!_)
    def attemptBump: SP[F, Boolean] = {
      for {
        phase <- currentBallotPhase(nodeId, slotIndex)
        result <- ifM(phase == Ballot.Phase.Externalize, false) {
          for {
            counters <- allCountersFromBallotEnvelopes(nodeId, slotIndex)
            r0 <- ifM(counters.isEmpty, false) {
              for {
                b <- currentBallot(nodeId, slotIndex)
                targetCounter = b.map(_.counter).getOrElse(0)
                r00 <- ifM(counters.head < targetCounter, false) {
                  for {
                    r000 <- ifM(isCounterNotAccepted(targetCounter), false) {
                      //(result, found not accepted):(Boolean, Boolean)
                      counters
                        .foldLeft((false, false).pureSP[F]) { (acc, n) =>
                          for {
                            pre <- acc
                            r0000 <- ifM(pre._2, acc) {
                              for {
                                notAccepted <- isCounterNotAccepted(n)
                                r00000 <- ifM(notAccepted,
                                              abandonBallot(nodeId, slotIndex, previousValue, n)
                                                .map((_, true)))(acc)
                              } yield r00000
                            }
                          } yield r0000
                        }
                        .map(_._1)
                    }
                  } yield r000
                }
              } yield r00
            }
          } yield r0
        }
      } yield result
    }
    // return bumped, but loop until return false
    def attemptBumpMore: SP[F, Boolean] = {
      def _loop(acc: Boolean): SP[F, Boolean] =
        for {
          b <- attemptBump
          _ <- ifM(b, attemptBump)(false)
        } yield acc || b

      _loop(false)
    }

    for {
      _                <- currentMessageLevelUp(nodeId, slotIndex)
      prepareAccepted  <- attemptAcceptPrepare(nodeId, slotIndex, previousValue, statement)
      prepareConfirmed <- attemptConfirmPrepare(nodeId, slotIndex, previousValue, statement)
      commitAccepted   <- attemptAcceptCommit(nodeId, slotIndex, previousValue, statement)
      commitConfirmed  <- attemptConfirmCommit(nodeId, slotIndex, previousValue, statement)
      didWork = prepareAccepted || prepareConfirmed || commitAccepted || commitConfirmed
      bumped <- ifM(currentMessageLevel(nodeId, slotIndex).map(_ == 1), attemptBumpMore)(false)
      _ <- ifThen(didWork || bumped) {
        for {
          unemitted <- currentUnemittedBallotMessage(nodeId, slotIndex)
          _         <- ifThen(unemitted.isDefined)(emit(nodeId, slotIndex, previousValue, unemitted.get))
        } yield ()
      }
      _ <- currentMessageLevelDown(nodeId, slotIndex)
    } yield ()
  }

  def attemptAcceptPrepare(nodeId: NodeID,
                           slotIndex: SlotIndex,
                           previousValue: Value,
                           hint: Statement[Message.BallotMessage]): SP[F, Boolean]
  def attemptConfirmPrepare(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            hint: Statement[Message.BallotMessage]): SP[F, Boolean]
  def attemptAcceptCommit(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          hint: Statement[Message.BallotMessage]): SP[F, Boolean]
  def attemptConfirmCommit(nodeId: NodeID,
                           slotIndex: SlotIndex,
                           previousValue: Value,
                           hint: Statement[Message.BallotMessage]): SP[F, Boolean]
}
