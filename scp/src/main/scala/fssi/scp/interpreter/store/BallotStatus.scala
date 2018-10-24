package fssi.scp.interpreter.store

import fssi.scp.types._

case class BallotStatus(
  phase: Var[Ballot.Phase]
)

object BallotStatus {
  def empty: BallotStatus = BallotStatus(
    phase = Var.empty
  )
  private val instances: Var[Map[(NodeID, SlotIndex), BallotStatus]] = Var(Map.empty)

  def getInstance(nodeId: NodeID, slotIndex: SlotIndex): BallotStatus = {
    // Var[Option[Ballot]]
    instances.map(_.get((nodeId, slotIndex))).map {
      case Some(b) => b
      case None =>
        val b = empty
        instances := instances.unsafe() + ((nodeId, slotIndex) -> b)
        b
    }.unsafe
  }
}
