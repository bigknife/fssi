package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleExternalizeProgram[F[_]] extends BaseProgram[F] {
  def handleExternalize(nodeId: NodeID,
                        slotIndex: SlotIndex,
                        previousValue: Value,
                        statement: Statement[Message.Externalize]): SP[F, Boolean] = ???
}
