package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait AttemptConfirmCommitProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def attemptConfirmCommit(nodeId: NodeID,
                           slotIndex: SlotIndex,
                           previousValue: Value,
                           hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    val ballotToExternalize = hint.message.externalizableBallot
    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      ifM(ballotToExternalize.isEmpty, true)(
        canConfirmCommitNow(nodeId, slotIndex, ballotToExternalize.get))

    def isCounterConfirmed(interval: CounterInterval, ballot: Ballot): SP[F, Boolean] = {
      for {
        acceptedNodes <- nodesAcceptedCommit(nodeId, slotIndex, ballot, interval)
        accepted <- isQuorum(nodeId, acceptedNodes)
        } yield accepted
    }

    def confirmedCommitCounterInterval(boundaries: CounterSet, ballot: Ballot): SP[F, CounterInterval] = {
      // Option[interval], everAccepted, lastAccepted
      val x = boundaries.foldRight((Option.empty[CounterInterval], false, false).pureSP[F]){(n, acc) =>
        for {
          pre <- acc
          next <- ifM(pre._2 && !pre._3, pre) {
            val interval = pre._1.map(_.withFirst(n)).getOrElse(CounterInterval(n))
            for {
              accepted <- isCounterConfirmed(interval, ballot)
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
      val ballot = ballotToExternalize.get

      for {
        boundaries <- commitBoundaries(nodeId, slotIndex, ballot)
        interval   <- confirmedCommitCounterInterval(boundaries, ballot)
        accepted <- ifM(interval.notAvailable, false) {
          val newC = Ballot(interval.first, ballot.value)
          val newH = Ballot(interval.second, ballot.value)
          confirmCommitted(nodeId, slotIndex, newC, newH)
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
