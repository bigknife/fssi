package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleAcceptNominationsProgram[F[_]] extends BaseProgram[F] {
  def handleAcceptNominations(nodeId: NodeID,
                              slotIndex: SlotIndex,
                              previousValue: Value,
                              statement: Statement[Message.AcceptNominations]): SP[F, Boolean] = ???
}
