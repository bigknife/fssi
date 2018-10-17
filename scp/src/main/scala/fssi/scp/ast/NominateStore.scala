package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import types._

@sp trait NominateStore[F[_]] {
  def canNominateNewValue(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean]
}
