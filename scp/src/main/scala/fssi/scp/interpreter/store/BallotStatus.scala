package fssi.scp.interpreter.store

import fssi.scp.types.Message.BallotMessage
import fssi.scp.types._

case class BallotStatus(
    heardFromQuorum: Var[Boolean],
    phase: Var[Ballot.Phase],
    currentBallot: Var[Ballot], //b
    prepared: Var[Option[Ballot]], //p
    preparedPrime: Var[Option[Ballot]], //p'
    highBallot: Var[Option[Ballot]], //h
    commit: Var[Option[Ballot]], //c
    latestEnvelopes: Var[Map[NodeID, Envelope[Message.BallotMessage]]], //M
    valueOverride: Var[Option[Value]], //z
    currentMessageLevel: Var[Int]
)

object BallotStatus {
  def empty: BallotStatus = BallotStatus(
    heardFromQuorum = Var(false),
    phase = Var(Ballot.Phase.Prepare),
    currentBallot = Var(Ballot.bottom),
    prepared = Var(None),
    preparedPrime = Var(None),
    highBallot = Var(None),
    commit = Var(None),
    latestEnvelopes = Var(Map.empty),
    valueOverride = Var(None),
    currentMessageLevel = Var(0)
  )
  private val instances: Var[Map[(NodeID, SlotIndex), BallotStatus]] = Var(Map.empty)

  def getInstance(nodeId: NodeID, slotIndex: SlotIndex): BallotStatus = {
    instances
      .map(_.get((nodeId, slotIndex)))
      .map {
        case Some(b) => b
        case None =>
          val b = empty
          instances := instances.unsafe() + ((nodeId, slotIndex) -> b)
          b
      }
      .unsafe
  }
  def cleanInstance(nodeId: NodeID, slotIndex: SlotIndex): Unit =
    instances.map(_.get((nodeId, slotIndex))).foreach {
      case Some(_) =>
        instances := instances.unsafe() - ((nodeId, slotIndex))
        ()
      case None =>
    }
}
