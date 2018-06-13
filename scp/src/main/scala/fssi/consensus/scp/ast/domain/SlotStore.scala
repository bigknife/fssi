package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait SlotStore[F[_]] {

  /** find a slot by index */
  def findSlot(index: Long): P[F, Option[Slot]]

  /** save a slot */
  def saveSlot(slot: Slot): P[F, Unit]

  /** save historical statements for a slot */
  def saveHistoricalStatement(
      slot: Slot,
      historicalStatement: Statement.HistoricalStatement): P[F, Unit]

  /** exist the value on the accepted values of the slot */
  def exitAcceptedValue(slot: Slot, value: Value): P[F, Boolean]
}
