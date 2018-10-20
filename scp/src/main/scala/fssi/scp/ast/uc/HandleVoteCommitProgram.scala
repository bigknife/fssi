package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleVoteCommitProgram[F[_]] extends EmitProgram[F] {
  import model.nodeService._
  import model.nodeStore._
  import model.applicationService._

  def handleVoteCommit(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.VoteCommit]): SP[F, Boolean] = {

    val ballot = statement.message.ballot
    // check message is sane, and,
    // check the ballot to see if it can be vote(commit(b))
    def checkStatement: SP[F, Boolean] =
      for {
        sane         <- isMessageSane(statement.message)
        canCommitted <- cannotBallotCommitted(nodeId, slotIndex, statement.message.ballot)
      } yield sane && canCommitted
    def statementCheckFailed = checkStatement.map(!_)

    def acceptedCounterIntervalFrom(counters: CounterSet): SP[F, CounterInterval] = {
      if (counters.isEmpty) CounterInterval().pureSP[F]
      else {
        val highest = counters.last
        // CounterInterval, acceptedEver, acceptedLast, CounterInterval_last_Accepted
        counters
          .dropRight(1)
          .foldRight((CounterInterval(highest), false, false, CounterInterval()).pureSP[F]) {
            (n, acc) =>
              for {
                pre <- acc
                (ci, ae, al, ci_last_accepted) = pre
                next <- ifM(ae && !al, acc) {
                  for {
                    acceptedNodes <- nodesAcceptedCommit(nodeId, slotIndex, ballot, ci)
                    accepted <- ifM(isVBlocking(nodeId, acceptedNodes), true) {
                      for {
                        votedNodes <- nodesVotedCommit(nodeId, slotIndex, ballot, ci)
                        byQuorum   <- isQuorum(nodeId, votedNodes ++ acceptedNodes)
                      } yield byQuorum
                    }
                    ci_next = if (ae) ci else ci.withFirst(n)
                  } yield
                    if (accepted) (ci_next, true, true, ci)
                    else (ci_next, ae, false, ci_last_accepted)
                }
              } yield next
          }.map(_._4)
      }
    }

    // find commit counter boundaries from local received statements
    //                       eg: <3,5,8,10,20, 30>
    // check the boundaries to get a highest interval by federated accepting
    //                       eg: [5, 20]
    // find federated accepted highest ballot and lowest ballot
    //   which should be compatible with the incoming ballot
    // AST:
    ifM(statementCheckFailed, false) {
      for {
        counters                <- findCompatibleCounterOfCommitted(nodeId, slotIndex, statement.message.ballot)
        acceptedCounterInterval <- acceptedCounterIntervalFrom(counters)
        _ <- ifThen(acceptedCounterInterval.first > 0) {
          val lowest  = Ballot(acceptedCounterInterval.first, statement.message.ballot.value)
          val highest = Ballot(acceptedCounterInterval.second, statement.message.ballot.value)
          for {

            _ <- ifThen(acceptCommitted(nodeId, slotIndex, lowest, highest)) {
              for {
                msg <- createAcceptCommitMessage(nodeId, slotIndex)
                _   <- emit(nodeId, slotIndex, previousValue, msg)
              } yield ()
            }
          } yield ()
        }
      } yield true

    }

  }
}
