package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait AttemptConfirmPrepareProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def attemptConfirmPrepare(nodeId: NodeID,
                            slotIndex: SlotIndex,
                            previousValue: Value,
                            hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      currentBallotPhase(nodeId, slotIndex).map(_ != Ballot.Phase.Prepare)

    def newHigestBallot(candidates: BallotSet): SP[F, Option[Ballot]] = {
      val commitable = candidates.foldLeft(BallotSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre            <- acc
          canBeRaisedToH <- canBallotBeHighestCommitPotentially(nodeId, slotIndex, n)
        } yield if (canBeRaisedToH) pre + n else pre
      }

      for {
        commitableCandidates <- commitable
        x <- commitableCandidates.foldRight(Option.empty[Ballot].pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre.isDefined, acc) {
              for {
                nodeAccepted <- nodesAcceptedPrepare(nodeId, slotIndex, n)
                confirmed    <- isQuorum(nodeId, nodeAccepted)
                result       <- ifM(confirmed, Option(n))(None)
              } yield result
            }
          } yield next
        }
      } yield x

    }

    def updateLocalState(ballot: Ballot): SP[F, Boolean] =
      updateBallotStateWhenAcceptPrepare(nodeId, slotIndex, ballot)

    def lowestCandidates(candidates: BallotSet, newH: Ballot): BallotSet = {
      def _loop(xs: BallotSet): BallotSet = {
        val h = xs.takeRight(1)
        if (h == newH) xs.dropRight(1)
        else _loop(xs.dropRight(1))
      }

      _loop(candidates)
    }

    def commitAsLowestBallots(candidates: BallotSet, newH: Ballot): SP[F, BallotSet] =
      // (ballotSet, stopFlat)
      candidates
        .foldRight((BallotSet.empty, false).pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre._2, acc) {
              for {
                x <- canBallotBeLowestCommitPotentially(nodeId, slotIndex, n, newH)
              } yield if (x) (pre._1 + n, false) else (pre._1, true)
            }
          } yield next
        }
        .map(_._1)

    //@see BallotProtocol.cpp#979, to find the lowest, fold from left
    def findLowestNewC(xs: BallotSet): SP[F, Option[Ballot]] =
      xs.foldLeft(Option.empty[Ballot].pureSP[F]) { (acc, n) =>
        for {
          pre <- acc
          next <- ifM(pre.isDefined, acc) {
            for {
              acceptedNodes <- nodesAcceptedPrepare(nodeId, slotIndex, n)
              q             <- isQuorum(nodeId, acceptedNodes)
            } yield if (q) Option(n) else None
          }
        } yield next
      }

    // AST:
    ifM(ignoreByCurrentPhase, false) {
      for {
        candidates <- prepareCandidatesWithHint(nodeId, slotIndex, hint)
        newH       <- newHigestBallot(candidates)
        newC <- ifM(newH.isEmpty, Option.empty[Ballot]) {
          ifM(notNeccessarySetLowestCommitBallotUnderHigh(nodeId, slotIndex, newH.get),
              Option.empty[Ballot]) {
            for {
              legalLowCommits <- commitAsLowestBallots(lowestCandidates(candidates, newH.get),
                                                       newH.get)
              c <- findLowestNewC(legalLowCommits)
            } yield c
          }
        }
        updated <- updateBallotStateWhenConfirmPrepare(nodeId, slotIndex, newH, newC)
        _ <- ifThen(updated) {
          for {
            msg <- createBallotMessage(nodeId, slotIndex)
            _   <- emit(nodeId, slotIndex, previousValue, msg)
          } yield ()
        }
      } yield updated
    }
  }
}
