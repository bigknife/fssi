package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAcceptPrepareProgram[F[_]] extends EmitProgram[F] {

  import model.applicationService._
  import model.nodeService._
  import model.nodeStore._

  def handleAcceptPrepare(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          statement: Statement[Message.AcceptPrepare]): SP[F, Boolean] = {

    // because BallotSet is totally ordered, so we can iterate them from
    // right side to get a highest, acceptable ballot.
    def findConfirmedBallot(xs: BallotSet): SP[F, Option[Ballot]] =
      xs.foldRight(Option.empty[Ballot].pureSP[F]) { (n, acc) =>
        for {
          pre <- acc
          x <- ifM(pre.isDefined, right = pre) {
            for {
              acceptedNodes <- nodesAcceptedPrepare(nodeId, slotIndex, n)
              votesNodes    <- nodesVotedPrepare(nodeId, slotIndex, n)
              confirmed     <- isQuorum(nodeId, votesNodes ++ acceptedNodes)
            } yield if (confirmed) Option(n) else None
          }
        } yield x
      }

    // AST:
    ifM(isMessageNotSane(statement.message), right = false) {
      ifM(needHandleMessage(nodeId, slotIndex, statement.message), right = true) {
        for {
          prepareCandidates <- findBallotsToPrepare(nodeId, slotIndex)
          confirmedBallot   <- findConfirmedBallot(prepareCandidates)
          _ <- ifThen(confirmedBallot.isDefined) {
            for {
              changed <- confirmPrepared(nodeId, slotIndex, confirmedBallot.get)
              _ <- ifThen(changed) {
                for {
                  _   <- stopNominating(nodeId, slotIndex)
                  msg <- createVoteCommit(nodeId, slotIndex)
                  _   <- emit(nodeId, slotIndex, previousValue, msg)
                } yield ()
              }
            } yield ()
          }
        } yield true
      }
    }
  }
}
