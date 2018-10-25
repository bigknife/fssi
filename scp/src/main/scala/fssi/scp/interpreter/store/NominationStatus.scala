package fssi.scp.interpreter.store
import fssi.scp.types._

case class NominationStatus(
    roundNumber: Var[Int],
    votes: Var[ValueSet], // X
    accepted: Var[ValueSet], // Y
    candidates: Var[ValueSet],                                         // Z
    latestNominations: Var[Map[NodeID, Envelope[Message.Nomination]]], // N
    // last envelope emitted by this node
    lastEnvelope: Var[Option[Envelope[Message.Nomination]]],
    // nodes from quorum set that have the highest priority this round
    rounderLeaders: Var[Set[NodeID]],
    // true when nominate started
    nominationStarted: Var[Boolean],
    // the latest candidate value
    latestCompositeCandidate: Var[Option[Value]],
    // the value from the previous slot
    previousValue: Var[Option[Value]]
)

object NominationStatus {

  def empty = NominationStatus(
    Var.empty,
    Var(ValueSet.empty),
    Var(ValueSet.empty),
    Var(ValueSet.empty),
    Var(Map.empty),
    Var(None),
    Var(Set.empty),
    Var(false),
    Var(None),
    Var(None)
  )

  private lazy val instances: Var[Map[(NodeID, SlotIndex), NominationStatus]] = Var(Map.empty)

  def getInstance(nodeID: NodeID, slotIndex: SlotIndex): NominationStatus = {
    instances
      .map(_.get((nodeID, slotIndex)))
      .map {
        case Some(n) => n
        case None =>
          val n = empty
          instances := instances.map(_ + ((nodeID, slotIndex) -> n)).unsafe()
          n
      }
      .unsafe()
  }

  def clearInstance(nodeID: NodeID, slotIndex: SlotIndex): Unit = {
    instances.map(_.get((nodeID, slotIndex))).foreach {
      case Some(_) => instances := instances.map(_ - ((nodeID, slotIndex))).unsafe()
      case None    =>
    }
  }
}
