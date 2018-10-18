package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAcceptCommitProgram[F[_]] extends BaseProgram[F] {
  def handleAcceptCommit(nodeId: NodeID,
                         slotIndex: SlotIndex,
                         previousValue: Value,
                         statement: Statement[Message.AcceptCommit]): SP[F, Boolean] = ???
}
