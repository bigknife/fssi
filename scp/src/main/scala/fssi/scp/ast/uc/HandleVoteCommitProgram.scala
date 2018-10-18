package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleVoteCommitProgram[F[_]] extends BaseProgram[F] {
  def handleVoteCommit(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.VoteCommit]): SP[F, Boolean] = ???
}
