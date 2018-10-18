package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAcceptPrepareProgram[F[_]] extends BaseProgram[F] {
  def handleAcceptPrepare(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          previousValue: Value,
                          statement: Statement[Message.AcceptPrepare]): SP[F, Boolean] = ???
}
