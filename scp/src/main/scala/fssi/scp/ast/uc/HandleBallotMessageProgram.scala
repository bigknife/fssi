package fssi.scp
package ast
package uc

import types._
import components._

import bigknife.sop._
import bigknife.sop.implicits._

trait HandleBallotMessageProgram[F[_]] extends SCP[F] with BaseProgram[F] {

  def handleBalootMessage(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       previousValue: Value,
                       statement: Statement[Message.BallotMessage]): SP[F, Boolean] = ???
}
