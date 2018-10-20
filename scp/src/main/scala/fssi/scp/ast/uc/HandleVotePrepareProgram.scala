package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleVotePrepareProgram[F[_]] extends EmitProgram[F] {

  import model.nodeService._
  import model.nodeStore._

  private[uc] def handleVotePrepare(nodeId: NodeID,
                        slotIndex: SlotIndex,
                        previousValue: Value,
    statement: Statement[Message.VotePrepare]): SP[F, Boolean] = {

    // because BallotSet is totally ordered, so we can iterate them from
    // right side to get a highest, acceptable ballot.
    def findAcceptableBallot(xs: BallotSet): SP[F, Option[Ballot]] =
      xs.foldRight(Option.empty[Ballot].pureSP[F]) {(n, acc) =>
        for {
          pre <- acc
          x <- ifM(pre.isDefined, right = pre) {
            for {
              acceptedNodes <- nodesAcceptedPrepare(nodeId, slotIndex, n)
              accepted <- ifM(isVBlocking(nodeId, acceptedNodes), true) {
                for {
                  votesNodes <- nodesVotedPrepare(nodeId, slotIndex, n)
                  byQuorum <- isQuorum(nodeId, votesNodes ++ acceptedNodes)
                } yield byQuorum
              }
            } yield if(accepted) Option(n) else None
          }
        } yield x
      }

    // AST:
    ifM(isMessageNotSane(statement.message), false) {
      ifM(needHandleMessage(nodeId, slotIndex, statement.message), right = true) {
        for {
          prepareCandidates <- findBallotsToPrepare(nodeId, slotIndex)
          acceptableBallot <- findAcceptableBallot(prepareCandidates)
          _ <- ifThen(acceptableBallot.isDefined) {
            for {
              changed <- acceptPrepared(nodeId, slotIndex, acceptableBallot.get)
              _ <- ifThen(changed) {
                for {
                  msg <- createAcceptPrepareMessage(nodeId, slotIndex)
                  _ <- emit(nodeId, slotIndex, previousValue, msg)
                } yield ()
              }
            } yield ()
          }
        } yield true
      }
    }
  }
}
