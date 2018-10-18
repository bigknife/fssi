package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleVotePrepareProgram[F[_]] extends BaseProgram[F] {
  def handleVotePrepare(nodeId: NodeID,
                        slotIndex: SlotIndex,
                        previousValue: Value,
                        statement: Statement[Message.VotePrepare]): SP[F, Boolean] = ???
}
