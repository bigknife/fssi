package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait AttemptAcceptCommitProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._
  import model.logService._

  def attemptAcceptCommit(slotIndex: SlotIndex,
                          previousValue: Value,
                          hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    val ballotToCommit = hint.message.commitableBallot

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      ifM(ballotToCommit.isEmpty, true)(for {
        canAccept <- canAcceptCommitNow(slotIndex, ballotToCommit.get)
      } yield !canAccept)

    def isCounterAccepted(interval: CounterInterval, ballot: Ballot): SP[F, Boolean] = {
      for {
        acceptedNodes <- nodesAcceptedCommit(slotIndex, ballot, interval)
        accepted <- ifM(isLocalVBlocking(acceptedNodes), true.pureSP[F]) {
          for {
            votedNodes <- nodesVotedCommit(slotIndex, ballot, interval)
            q          <- isLocalQuorum(votedNodes ++ acceptedNodes)
          } yield q
        }
      } yield accepted
    }

    def acceptedCommitCounterInterval(boundaries: CounterSet,
                                      ballot: Ballot): SP[F, CounterInterval] = {
      // Option[interval], everAccepted, lastAccepted
      val x = boundaries.foldRight((Option.empty[CounterInterval], false, false).pureSP[F]) {
        (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre._2 && !pre._3, pre) {
              val currentInterval = pre._1.map(_.withFirst(n)).getOrElse(CounterInterval(n))
              for {
                accepted <- isCounterAccepted(currentInterval, ballot)
                _ <- if (accepted)
                  info(
                    s"[$slotIndex][AttemptAcceptCommit] accept interval: $currentInterval, $ballot")
                else
                  info(
                    s"[$slotIndex][AttemptAcceptCommit] reject interval: $currentInterval, $ballot")
              } yield
                if ((pre._1.isEmpty || pre._2) && accepted) (Option(currentInterval), accepted, accepted)
                else pre
            }
          } yield next
      }
      x.map {
        case (Some(interval), true, _) => interval

        case _ => CounterInterval()
      }
    }

    ifM(ignoreByCurrentPhase, false.pureSP[F]) {
      val ballot: Ballot = ballotToCommit.get
      for {
        phase      <- currentBallotPhase(slotIndex)
        boundaries <- commitBoundaries(slotIndex, ballot)
        _          <- info(s"[$slotIndex][AttemptAcceptCommit] found boundaries $boundaries at phase $phase")
        interval   <- acceptedCommitCounterInterval(boundaries, ballot)
        _          <- info(s"[$slotIndex][AttemptAcceptCommit] found accepted interval: $interval")
        accepted <- ifM(interval.notAvailable, false) {
          val newC = Ballot(interval.first, ballot.value)
          val newH = Ballot(interval.second, ballot.value)
          for {
            x <- acceptCommitted(slotIndex, newC, newH)
            _ <- info(s"[$slotIndex][AttemptAcceptCommit] accept ($newC - $newH), $x")
          } yield x

        }
        phaseNow <- currentBallotPhase(slotIndex)
        _ <- ifThen(phase == Ballot.Phase.Prepare && phaseNow == Ballot.Phase.Confirm) {
          for {
            _ <- info(s"[$slotIndex][AttemptAcceptCommit] phase upgraded to Confirm")
            c <- currentConfirmedBallot(slotIndex)
            _ <- phaseUpgradeToConfirm(slotIndex, c)
          } yield ()
        }
        _ <- ifThen(accepted) {
          for {
            msg <- createBallotMessage(slotIndex)
            _   <- emitBallot(slotIndex, previousValue, msg)
          } yield ()
        }
      } yield accepted
    }
  }
}
