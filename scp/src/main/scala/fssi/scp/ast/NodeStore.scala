package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.scp.types.Message.BallotMessage

import scala.collection.immutable._
import types._

@sp trait NodeStore[F[_]] {

  /** check the envelope to see if it's newer than local cache
    */
  def isNewerEnvelope[M <: Message](nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    envelope: Envelope[M]): P[F, Boolean]
  def isOlderEnvelope[M <: Message](nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    envelope: Envelope[M]): P[F, Boolean] =
    isNewerEnvelope(nodeId, slotIndex, envelope).map(!_)

  /** save new envelope, if it's a nomination message, save it into NominationStorage,
    * if it's a ballot message, save it into BallotStorage.
    */
  def saveEnvelope[M <: Message](nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 envelope: Envelope[M]): P[F, Unit]

  /** get nomination envelope from locally stored, received from peer nodes
    */
  def getNominationEnvelope(slotIndex: SlotIndex,
                            peerNodeId: NodeID): P[F, Option[Envelope[Message.Nomination]]]

  /** remove an envelope
    */
  def removeEnvelope[M <: Message](nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   envelope: Envelope[M]): P[F, Unit]

  /** find not accepted (nominate x) from values
    *
    * @param values given a value set
    * @return a subset of values, the element in which is not accepted as nomination value
    */
  def notAcceptedNominatingValues(slotIndex: SlotIndex, values: ValueSet): P[F, ValueSet]

  /** find current accepted nomination votes
    */
  def acceptedNominations(slotIndex: SlotIndex): P[F, ValueSet]

  def hasNominationValueAccepted(slotIndex: SlotIndex, value: Value): P[F, Boolean]

  /** find current candidates nomination value
    */
  def candidateNominations(slotIndex: SlotIndex): P[F, ValueSet]
  def haveCandidateNominations(slotIndex: SlotIndex): P[F, Boolean] =
    candidateNominations(slotIndex).map(_.nonEmpty)

  def isLeader(nodeID: NodeID, slotIndex: SlotIndex): P[F, Boolean]
  def isNotLeader(nodeID: NodeID, slotIndex: SlotIndex): P[F, Boolean] =
    isLeader(nodeID, slotIndex).map(leader => !leader)

  /** save new values to current voted nominations
    */
  def voteNewNominations(slotIndex: SlotIndex, newVotes: ValueSet): P[F, Unit]

  /** the set of nodes which have voted(nominate x)
    */
  def nodesVotedNomination(slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** the set of nodes which have accept(nominate x)
    */
  def nodesAcceptedNomination(slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** save a new value to current accepted nominations
    */
  def acceptNewNomination(slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** save a new value to current candidated nominations
    */
  def candidateNewNomination(slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** get current ballot
    */
  def currentBallot(slotIndex: SlotIndex): P[F, Option[Ballot]]

  /** given a ballot, get next ballot to try base on local state (z)
    * if there is a value stored in z, use <z, counter>, or use <attempt, counter>
    */
  def nextBallotToTry(slotIndex: SlotIndex, attempt: Value, counter: Int): P[F, Ballot]

  /** update local state when a new ballot was bumped into
    *
    * @see BallotProtocol.cpp#399
    */
  def updateBallotStateWhenBumpNewBallot(slotIndex: SlotIndex, newB: Ballot): P[F, Boolean]

  /** update local state when a ballot would be accepted as being prepared
    *
    * @see BallotProtocol.cpp#879
    */
  def updateBallotStateWhenAcceptPrepare(slotIndex: SlotIndex, newP: Ballot): P[F, Boolean]

  /** update local state when a new high ballot and a new low ballot would be confirmed as being prepared
    *
    * @see BallotProtocol.cpp#1031
    */
  def updateBallotStateWhenConfirmPrepare(slotIndex: SlotIndex,
                                          newH: Option[Ballot],
                                          newC: Option[Ballot]): P[F, Boolean]

  /** check received ballot envelope, find nodes which are ahead of local node
    */
  def nodesAheadLocal(slotIndex: SlotIndex): P[F, Set[NodeID]]

  /** find nodes ballot is ahead of a counter n
    *
    * @see BallotProtocol#1385
    */
  def nodesAheadBallotCounter(slotIndex: SlotIndex, counter: Int): P[F, Set[NodeID]]

  /** set heard from quorum
    */
  def heardFromQuorum(slotIndex: SlotIndex, heard: Boolean): P[F, Unit]

  /** check heard from quorum
    */
  def isHeardFromQuorum(slotIndex: SlotIndex): P[F, Boolean]

  def ballotDidHearFromQuorum(slotIndex: SlotIndex): P[F, Unit]

  /** get current ballot phase
    */
  def currentBallotPhase(slotIndex: SlotIndex): P[F, Ballot.Phase]

  /** get `c` in local state
    */
  def currentCommitBallot(slotIndex: SlotIndex): P[F, Option[Ballot]]

  /** get current message level
    * message level is used to control `attempBump` only bening invoked once when advancing ballot which
    * would cause recursive-invoking.
    */
  def currentMessageLevel(slotIndex: SlotIndex): P[F, Int]
  def currentMessageLevelUp(slotIndex: SlotIndex): P[F, Unit]
  def currentMessageLevelDown(slotIndex: SlotIndex): P[F, Unit]

  /** find all counters from received ballot message envelopes
    *
    * @see BallotProtocol.cpp#1338
    */
  def allCountersFromBallotEnvelopes(slotIndex: SlotIndex): P[F, CounterSet]

  /** get un emitted ballot message
    */
  def currentUnEmittedBallotMessage(slotIndex: SlotIndex): P[F, Option[Message.BallotMessage]]

  /** find candidate ballot to prepare from local stored envelopes received from other peers
    * if the ballot is prepared, should be ignored.
    *
    * @see BallotProtocol.cpp#getPrepareCandidates
    */
  def prepareCandidatesWithHint(slotIndex: SlotIndex,
                                hint: Statement[Message.BallotMessage]): P[F, BallotSet]

  /** the set of nodes which have vote(prepare b)
    *
    * @see BallotProtocol.cpp#839-866
    */
  def nodesVotedPrepare(slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** the set of nodes which have accepted(prepare b)
    *
    * @see BallotProtocol.cpp#1521
    */
  def nodesAcceptedPrepare(slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** find all the commitable counters in recieved envelopes
    *
    * @see BallotProtocol.cpp#1117
    */
  def commitBoundaries(slotIndex: SlotIndex, ballot: Ballot): P[F, CounterSet]

  /** the set of nodes which have voted vote(commit b)
    */
  def nodesVotedCommit(slotIndex: SlotIndex,
                       ballot: Ballot,
                       counterInterval: CounterInterval): P[F, Set[NodeID]]

  /** the set of nodes which have accepted vote(commit b)
    */
  def nodesAcceptedCommit(slotIndex: SlotIndex,
                          ballot: Ballot,
                          counterInterval: CounterInterval): P[F, Set[NodeID]]

  /** accept ballots(low and high) as committed
    *
    * @see BallotProtocol.cpp#1292
    */
  def acceptCommitted(slotIndex: SlotIndex, lowest: Ballot, highest: Ballot): P[F, StateChanged]

  /** confirm ballots(low and high) as committed
    *
    * @see BallotProtocol.cpp#1292
    */
  def confirmCommitted(slotIndex: SlotIndex, lowest: Ballot, highest: Ballot): P[F, StateChanged]

  /** check if it's able to accept commit a ballot now
    *
    * @see BallotProtocol.cpp#1169-1172, 1209-1215
    */
  def canAcceptCommitNow(slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean]

  /** check if it's able to confirm commit a ballot now
    *
    * @see BallotProtocol.cpp#1434-1443, 1470-1473
    */
  def canConfirmCommitNow(slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean]

  /** get current confirmed ballot
    */
  def currentConfirmedBallot(slotIndex: SlotIndex): P[F, Ballot]

  /** get current nominating round
    */
  def currentNominateRound(slotIndex: SlotIndex): P[F, Int]

  /** set nominate round to the next one
    */
  def gotoNextNominateRound(slotIndex: SlotIndex): P[F, Unit]

  /** save new values to current accepted nominations
    */
  def acceptNewNominations(slotIndex: SlotIndex, values: ValueSet): P[F, Unit]

  /** find latest candidate value
    */
  def currentCandidateValue(slotIndex: SlotIndex): P[F, Option[Value]]

  /** modify current candidate value
    */
  def candidateValueUpdated(slotIndex: SlotIndex, composite: Value): P[F, Unit]

  /** check if a envelope can be emitted
    */
  def canEmit[M <: Message](slotIndex: SlotIndex, envelope: Envelope[M]): P[F, Boolean]

  def envelopeReadyToBeEmitted[M <: Message](slotIndex: SlotIndex,
                                             envelope: Envelope[M]): P[F, Unit]

  def shouldProcess[M <: Message](slotIndex: SlotIndex, envelope: Envelope[M]): P[F, Boolean]

  def localNode(): P[F, NodeID]

  def nominateEnvelope(slotIndex: SlotIndex): P[F, Option[Envelope[Message.Nomination]]]

  def ballotEnvelope(slotIndex: SlotIndex): P[F, Option[Envelope[Message.BallotMessage]]]

  def nominationsReceived(slotIndex: SlotIndex): P[F, Map[NodeID, Envelope[Message.Nomination]]]

  def currentSlotIndex(): P[F, SlotIndex]

  def newSlotIndex(slotIndex: SlotIndex): P[F, Unit]
}
