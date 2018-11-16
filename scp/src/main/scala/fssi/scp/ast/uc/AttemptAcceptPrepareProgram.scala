package fssi.scp
package ast
package uc

import bigknife.sop._
import bigknife.sop.implicits._
import fssi.scp.types._

trait AttemptAcceptPrepareProgram[F[_]] extends SCP[F] with EmitProgram[F] {
  import model.logService._
  import model.nodeService._
  import model.nodeStore._

  def attemptAcceptPrepare(slotIndex: SlotIndex,
                           previousValue: Value,
                           hint: Statement[Message.BallotMessage]): SP[F, Boolean] = {

    lazy val ignoreByCurrentPhase: SP[F, Boolean] =
      currentBallotPhase(slotIndex).map(_ == Ballot.Phase.Externalize)

    def tryAcceptHighestBallot(nodeID: NodeID,
                               candidates: BallotSet,
                               phase: Ballot.Phase): SP[F, Boolean] =
      candidates
        .foldRight(Option.empty[Ballot].pureSP[F]) { (n, acc) =>
          for {
            pre <- acc
            next <- ifM(pre.isDefined, pre) {
              for {
                canBePrepared <- ifM(ballotCannotBePrepared(slotIndex, n), false.pureSP[F]) {
                  for {
                    nodeAccepted <- nodesAcceptedPrepare(slotIndex, n)
                    accepted <- ifM(isVBlocking(nodeID, nodeAccepted), true.pureSP[F]) {
                      for {
                        nodeVoted <- nodesVotedPrepare(slotIndex, n)
                        byQuorum  <- isQuorum(nodeID, nodeVoted ++ nodeAccepted)
                      } yield byQuorum
                    }
                  } yield accepted
                }
                next <- ifM(!canBePrepared, pre) {
                  for {

                    updated <- updateBallotStateWhenAcceptPrepare(slotIndex, n)
                    _       <- info(s"[$slotIndex][AttemptAcceptPrepare] updated acceptable $n: $updated")
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
        localNode       <- localNode()
        candidates      <- prepareCandidatesWithHint(slotIndex, hint)
        _               <- info(s"[$slotIndex][AttemptAcceptPrepare] found prepare candidates: $candidates")
        phase           <- currentBallotPhase(slotIndex)
        highestAccepted <- tryAcceptHighestBallot(localNode, candidates, phase)
        _ <- info(
          s"[$slotIndex][AttemptAcceptPrepare] accepted highest candidates at phase:$phase: $highestAccepted")
        _ <- ifThen(highestAccepted) {
          for {
            msg <- createBallotMessage(slotIndex)
            _   <- emitBallot(slotIndex, previousValue, msg)
          } yield ()
        }
      } yield highestAccepted
    }
  }
}
