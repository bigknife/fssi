package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleNominationProgram[F[_]] extends SCP[F] with BaseProgram[F] {
  def handleNomination(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.Nomination]): SP[F, Boolean] = ???
}
