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
    roundLeaders: Var[Set[NodeID]],
    // true when nominate started
    nominationStarted: Var[Boolean],
    // the latest candidate value
    latestCompositeCandidate: Var[Option[Value]],
    // the value from the previous slot
    previousValue: Var[Option[Value]]
)

object NominationStatus {

  def empty = NominationStatus(
    roundNumber = Var(0),
    votes = Var(ValueSet.empty),
    accepted = Var(ValueSet.empty),
    candidates = Var(ValueSet.empty),
    latestNominations = Var(Map.empty),
    lastEnvelope = Var(Option.empty),
    roundLeaders = Var(Set.empty),
    nominationStarted = Var(false),
    latestCompositeCandidate = Var(Option.empty),
    previousValue = Var(Option.empty)
  )

  private lazy val instances: Var[Map[SlotIndex, NominationStatus]] = Var(Map.empty)

  def getInstance(slotIndex: SlotIndex): NominationStatus = {
    instances
      .map(_.get(slotIndex))
      .map {
        case Some(n) => n
        case None =>
          val n = empty
          instances := instances.map(_ + (slotIndex -> n)).unsafe()
          n
      }
      .unsafe()
  }

  def clearInstance(slotIndex: SlotIndex): Unit = {
    instances.map(_.get(slotIndex)).foreach {
      case Some(_) => instances := instances.map(_ - slotIndex).unsafe()
      case None    =>
    }
  }
}
