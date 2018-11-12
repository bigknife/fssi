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
  import model.logService._

  def attemptConfirmPrepare(slotIndex: SlotIndex,
                            previousValue: Value,
                            hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      currentBallotPhase(slotIndex).map(_ != Ballot.Phase.Prepare)

    def newHighestBallot(candidates: BallotSet): SP[F, Option[Ballot]] = {
      val commitable = candidates.foldLeft(BallotSet.empty.pureSP[F]) { (acc, n) =>
        for {
          pre            <- acc
          canBeRaisedToH <- canBallotBeHighestCommitPotentially(slotIndex, n)
        } yield if (canBeRaisedToH) pre + n else pre
      }

      for {
        commitableCandidates <- commitable
        x <- commitableCandidates.foldRight(Option.empty[Ballot].pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre.isDefined, pre) {
              for {
                nodeAccepted <- nodesAcceptedPrepare(slotIndex, n)
                confirmed    <- isLocalQuorum(nodeAccepted)
                result       <- ifM(confirmed, Option(n))(Option.empty[Ballot].pureSP[F])
              } yield result
            }
          } yield next
        }
      } yield x

    }


    // filter the illegal here ballots
    def commitAsLowestBallots(candidates: BallotSet, newH: Ballot): SP[F, BallotSet] =
      // (ballotSet, stopFlat)
      candidates
        .foldRight((BallotSet.empty, false).pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre._2, pre) {
              for {
                x <- canBallotBeLowestCommitPotentially(slotIndex, n, newH)
              } yield if (x) (pre._1 + n, false) else (pre._1, true)
            }
          } yield next
        }
        .map(_._1)

    /** @see BallotProtocol.cpp#979, to find the lowest, it's not enough to
      * fold from left, because the ballot above the lowest one had been ratified
      * still not be ratified now.
      */
    def findLowestNewC(xs: BallotSet): SP[F, Option[Ballot]] =
      //(Option[Ballot], Boolean): (the lowest, last ratified)
      xs.foldRight((Option.empty[Ballot], false).pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre._1.isDefined && !pre._2, pre) {
              for {
                acceptedNodes <- nodesAcceptedPrepare(slotIndex, n)
                q             <- isLocalQuorum(acceptedNodes)
              } yield if (q) (Option(n), true) else (pre._1, false)
            }
          } yield next
        }
        .map(_._1)

    // AST:
    ifM(ignoreByCurrentPhase, false.pureSP[F]) {
      for {
        candidates <- prepareCandidatesWithHint(slotIndex, hint)
        _ <- info(
          s"[$slotIndex][AttemptConfirmPrepare] found prepare candidates: $candidates")
        newH <- newHighestBallot(candidates)
        _    <- info(s"[$slotIndex][AttemptConfirmPrepare] found newH: $newH")
        newC <- ifM(newH.isEmpty, Option.empty[Ballot]) {
          ifM(notNecessarySetLowestCommitBallotUnderHigh(slotIndex, newH.get),
              Option.empty[Ballot].pureSP[F]) {
            for {
              legalLowCommits <- commitAsLowestBallots(candidates, newH.get)
              c <- findLowestNewC(legalLowCommits)
            } yield c
          }
        }
        _       <- info(s"[$slotIndex][AttemptConfirmPrepare] found newC: $newC")
        updated <- updateBallotStateWhenConfirmPrepare(slotIndex, newH, newC)
        _ <- info(
          s"[$slotIndex][AttemptConfirmPrepare] updated when confirm parepare: $updated")
        _ <- ifThen(updated) {
          for {
            msg <- createBallotMessage(slotIndex)
            _   <- emitBallot(slotIndex, previousValue, msg)
          } yield ()
        }
      } yield updated
    }
  }
}
