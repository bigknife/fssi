package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait SlotStore[F[_]] {
  /** find a slot by index */
  def findSlot(index: Long): P[F, Option[Slot]]

  def saveSlot(slot: Slot): P[F, Unit]
}
