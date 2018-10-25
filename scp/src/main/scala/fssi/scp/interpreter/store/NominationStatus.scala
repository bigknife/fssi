package fssi.scp.interpreter.store
import fssi.scp.types._

import scala.collection.immutable.TreeSet

case class NominationStatus(
    roundNumber: Var[Int],
    votes: Var[TreeSet[Value]], // X
    accepted: Var[TreeSet[Value]], // Y
    candidates: Var[TreeSet[Value]],                                   // Z
    latestNominations: Var[Map[NodeID, Envelope[Message.Nomination]]], // N
    // last envelope emitted by this node
    lastEnvelope: Var[Envelope[Message.Nomination]],
    // nodes from quorum set that have the highest priority this round
    rounderLeaders: Var[TreeSet[NodeID]],
    // true when nominate started
    nominationStarted: Var[Boolean],
    // the latest candidate value
    latestCompositeCandidate: Var[Value],
    // the value from the previous slot
    previousValue: Var[Value]
)

object NominationStatus {

  def empty =
    NominationStatus(Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty,
                     Var.empty)

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
    instances.map(_.get((nodeID, slotIndex))).unsafe() match {
      case Some(_) => instances := instances.map(_ - ((nodeID, slotIndex))).unsafe()
      case None    =>
    }
  }
}
