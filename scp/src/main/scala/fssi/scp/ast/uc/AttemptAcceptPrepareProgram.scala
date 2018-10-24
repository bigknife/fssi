package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait AttemptAcceptPrepareProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._
  import model.logService._

  def attemptAcceptPrepare(nodeId: NodeID,
                           slotIndex: SlotIndex,
                           previousValue: Value,
                           hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      currentBallotPhase(nodeId, slotIndex).map(_ == Ballot.Phase.Externalize)

    def tryAcceptHighestBallot(candidates: BallotSet, phase: Ballot.Phase): SP[F, Boolean] =
      candidates
        .foldRight(Option.empty[Ballot].pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre.isDefined, pre) {
              for {
                canBePrepared <- ifM(ballotCannotBePrepared(nodeId, slotIndex, n), false.pureSP[F]) {
                  for {
                    nodeAccepted <- nodesAcceptedPrepare(nodeId, slotIndex, n)
                    accepted <- ifM(isVBlocking(nodeId, nodeAccepted), true.pureSP[F]) {
                      for {
                        nodeVoted <- nodesVotedPrepare(nodeId, slotIndex, n)
                        byQuorum  <- isQuorum(nodeId, nodeVoted ++ nodeAccepted)
                      } yield byQuorum
                    }
                  } yield accepted
                }
                next <- ifM(!canBePrepared, pre) {
                  for {

                    updated <- updateBallotStateWhenAcceptPrepare(nodeId, slotIndex, n)
                    _ <- info(
                      s"[$nodeId][$slotIndex][AttemptAcceptPrepare] updated acceptable $n: $updated")
                  } yield if (updated) Option(n) else pre
                }
              } yield next
            }

          } yield next
        }
        .map(_.isDefined)

    // AST:
    ifM(ignoreByCurrentPhase, false.pureSP[F]) {
      for { 
        candidates <- prepareCandidatesWithHint(nodeId, slotIndex, hint)
        _ <- info(
          s"[$nodeId][$slotIndex][AttemptAcceptPrepare] found prepare candidates: $candidates")
        phase           <- currentBallotPhase(nodeId, slotIndex)
        highestAccepted <- tryAcceptHighestBallot(candidates, phase)
        _ <- info(
          s"[$nodeId][$slotIndex][AttemptAcceptPrepare] accepted higest candidates at phase:$phase: $highestAccepted")
        _ <- ifThen(highestAccepted) {
          for {
            msg <- createBallotMessage(nodeId, slotIndex)
            _   <- emit(nodeId, slotIndex, previousValue, msg)
          } yield ()
        }
     } yield highestAccepted
    }
  }
}
