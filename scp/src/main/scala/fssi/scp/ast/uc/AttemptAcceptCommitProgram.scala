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

  def attemptAcceptCommit(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    val ballotToCommit = hint.message.commitableBallot

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      ifM(ballotToCommit.isEmpty, true)(
        canAcceptCommitNow(nodeId, slotIndex, ballotToCommit.get): SP[F, Boolean])

    def isCounterAccepted(interval: CounterInterval, ballot: Ballot): SP[F, Boolean] = {
      for {
        acceptedNodes <- nodesAcceptedCommit(nodeId, slotIndex, ballot, interval)
        accepted <- ifM(isVBlocking(nodeId, acceptedNodes), true.pureSP[F]) {
          for {
            votedNodes <- nodesVotedCommit(nodeId, slotIndex, ballot, interval)
            q          <- isQuorum(nodeId, votedNodes ++ acceptedNodes)
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
              val interval = pre._1.map(_.withFirst(n)).getOrElse(CounterInterval(n))
              for {
                accepted <- isCounterAccepted(interval, ballot)
                _ <- info(
                  s"[$nodeId][$slotIndex][AttemptAcceptCommit] accepted interval: $interval, $ballot, $accepted")
              } yield (Option(interval), pre._2 || accepted, accepted)
            }
          } yield next
      }
      x.map {
        case (Some(interval), true, false) => interval

        case _ => CounterInterval()
      }
    }

    ifM(ignoreByCurrentPhase, false.pureSP[F]) {
      val ballot: Ballot = ballotToCommit.get
      for {
        phase      <- currentBallotPhase(nodeId, slotIndex)
        boundaries <- commitBoundaries(nodeId, slotIndex, ballot)
        _ <- info(
          s"[$nodeId][$slotIndex][AttemptAcceptCommit] found boundaries $boundaries at phase $phase")
        interval <- acceptedCommitCounterInterval(boundaries, ballot)
        _        <- info(s"[$nodeId][$slotIndex][AttemptAcceptCommit] found accepted interval: $interval")
        accepted <- ifM(interval.notAvailable, false) {
          val newC = Ballot(interval.first, ballot.value)
          val newH = Ballot(interval.second, ballot.value)
          for {
            x <- acceptCommitted(nodeId, slotIndex, newC, newH)
            _ <- info(s"[$nodeId][$slotIndex][AttemptAcceptCommit] accept ($newC - $newH), $x")
          } yield x

        }
        phaseNow <- currentBallotPhase(nodeId, slotIndex)
        _ <- ifThen(phase == Ballot.Phase.Prepare && phaseNow == Ballot.Phase.Confirm) {
          for {
            _ <- info(s"[$nodeId][$slotIndex][AttemptAcceptCommit] phase upgraded to Confirm")
            c <- currentConfirmedBallot(nodeId, slotIndex)
            _ <- phaseUpgradeToConfirm(nodeId, slotIndex, c)
          } yield ()
        }
        _ <- ifThen(accepted) {
          for {
            msg <- createBallotMessage(nodeId, slotIndex)
            _   <- emit(nodeId, slotIndex, previousValue, msg)
          } yield ()
        }
      } yield accepted
    }
  }
}
