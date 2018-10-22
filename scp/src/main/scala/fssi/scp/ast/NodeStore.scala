package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

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

  /** remove an envelope
    */
  def removeEnvelope[M <: Message](nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   envelope: Envelope[M]): P[F, Unit]

  /** find not accepted (nominate x) from values
    * @param values given a value set
    * @return a subset of values, the element in which is not accepted as nomination value
    */
  def notAcceptedNominatingValues(nodeId: NodeID,
                                  slotIndex: SlotIndex,
                                  values: ValueSet): P[F, ValueSet]

  /** find current accepted nomination votes
    */
  def acceptedNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]

  /** find current candidates nomination value
    */
  def candidateNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]
  def noCandidateNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean] =
    candidateNominations(nodeId, slotIndex).map(_.isEmpty)
  def haveCandidateNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean] =
    candidateNominations(nodeId, slotIndex).map(_.nonEmpty)

  /** save new values to current voted nominations
    */
  def voteNewNominations(nodeId: NodeID, slotIndex: SlotIndex, newVotes: ValueSet): P[F, Unit]

  /** the set of nodes which have voted(nominate x)
    */
  def nodesVotedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** the set of nodes which have accept(nominate x)
    */
  def nodesAcceptedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** save a new value to current accepted nominations
    */
  def acceptNewNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** save a new value to current candidated nominations
    */
  def candidateNewNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** get current ballot
    */
  def currentBallot(nodeId: NodeID, slotIndex: SlotIndex): P[F, Option[Ballot]]

  /** given a ballot, get next ballot to try base on local state (z)
    * if there is a value stored in z, use <z, counter>, or use <attempt, counter>
    */
  def nextBallotToTry(nodeId: NodeID,
                      slotIndex: SlotIndex,
                      attempt: Value,
                      counter: Int): P[F, Ballot]

  /** update local state based to the specified ballot
    */
  def updateBallotState(nodeId: NodeID, slotIndex: SlotIndex, newB: Ballot): P[F, Boolean]

  /** check received ballot envelope, find nodes which are ahead of local node
    */
  def nodesAheadLocal(nodeId: NodeID, slotIndex: SlotIndex): P[F, Set[NodeID]]

  /** find nodes ballot is ahead of a counter n
    * @see BallotProtocol#1385
    */
  def nodesAheadBallotCounter(nodeId: NodeID, slotIndex: SlotIndex, counter: Int): P[F, Set[NodeID]]

  /** set heard from quorum
    */
  def heardFromQuorum(nodeId: NodeID, slotIndex: SlotIndex, heard: Boolean): P[F, Unit]

  /** check heard from quorum
    */
  def isHeardFromQuorum(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean]

  /** get current ballot phase
    */
  def currentBallotPhase(nodeId: NodeID, slotIndex: SlotIndex): P[F, Ballot.Phase]

  /** get `c` in local state
    */
  def currentCommitBallot(nodeId: NodeID, slotIndex: SlotIndex): P[F, Option[Ballot]]

  /** get current message level
    * message level is used to control `attempBump` only bening invoked once when advancing ballot which 
    * would cause recursive-invoking.
    */
  def currentMessageLevel(nodeId: NodeID, slotIndex: SlotIndex): P[F, Int]
  def currentMessageLevelUp(nodeId: NodeID, slotIndex: SlotIndex): P[F, Unit]
  def currentMessageLevelDown(nodeId: NodeID, slotIndex: SlotIndex): P[F, Unit]

  /** find all counters from received ballot message envelopes
    * @see BallotProtocol.cpp#1338
    */
  def allCountersFromBallotEnvelopes(nodeId: NodeID, slotIndex: SlotIndex): P[F, CounterSet]

  /** get un emitted ballot message 
    */
  def currentUnemittedBallotMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Option[Message.BallotMessage]]

  //////////////////////////////////////////////////////////////////////////////////////////////////////
  /** get current nominating round
    */
  def currentNominateRound(nodeId: NodeID, slotIndex: SlotIndex): P[F, Int]

  /** set nominate round to the next one
    */
  def gotoNextNominateRound(nodeId: NodeID, slotIndex: SlotIndex): P[F, Unit]

  /** save new values to current accepted nominations
    */
  def acceptNewNominations(nodeId: NodeID, slotIndex: SlotIndex, values: ValueSet): P[F, Unit]

  /** find current un-accepted votes values (vote(nominate x))
    */
  def unAcceptedNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]

  /** nominate a value as candidate value
    */
  def candidateNewValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** find latest candidate value
    */
  def currentCandidateValue(nodeId: NodeID, slotIndex: SlotIndex): P[F, Option[Value]]

  /** the set of nodes which have vote(prepare b)
    */
  def nodesVotedPrepare(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** the set of nodes which have accepted(prepare b)
    */
  def nodesAcceptedPrepare(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** the set of nodes which have voted vote(commit b)
    */
  def nodesVotedCommit(nodeId: NodeID,
                       slotIndex: SlotIndex,
                       ballot: Ballot,
                       counterInterval: CounterInterval): P[F, Set[NodeID]]

  /** the set of nodes which have accepted vote(commit b)
    */
  def nodesAcceptedCommit(nodeId: NodeID,
                          slotIndex: SlotIndex,
                          ballot: Ballot,
                          counterInterval: CounterInterval): P[F, Set[NodeID]]

  /** save latest message sent from any node. SHOULD saved by different message type.
    */
  def saveLatestStatement[M <: Message](nodeId: NodeID,
                                        slotIndex: SlotIndex,
                                        statement: Statement[M]): P[F, Unit]

  /** compare the message with local cached message
    */
  def isStatementNewer[M <: Message](nodeId: NodeID,
                                     slotIndex: SlotIndex,
                                     statement: Statement[M]): P[F, Boolean]

  /** check if a value has been stored as votes or accepted.
    */
  def valueVotedOrAccepted(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Boolean]

  /** check if a value has been stored as accepted
    */
  def valueAccepted(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Boolean]

  /** find candidate ballot to prepare from local stored envelopes received from other peers
    * if the ballot is prepared, should be ignored.
    * @see BallotProtocol.cpp#getPrepareCandidates
    */
  def findBallotsToPrepare(nodeId: NodeID, slotIndex: SlotIndex): P[F, BallotSet]

  /** set prepared ballot to accepted for local states, if any states changed, return true, or else return false
    */
  def acceptPrepared(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, StateChanged]

  /** set prepared ballot to confirmed for local state, if any states changed, return true, or else return false
    */
  def confirmPrepared(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, StateChanged]

  /** accepted ballots(low and high) as committed
    */
  def acceptCommitted(nodeId: NodeID,
                      slotIndex: SlotIndex,
                      lowest: Ballot,
                      highest: Ballot): P[F, StateChanged]

  /** check if a envelope can be emitted
    */
  def canEmit[M <: Message](nodeId: NodeID,
                            slotIndex: SlotIndex,
                            envelope: Envelope[M]): P[F, Boolean]

  /** find all committed counters from envelopes received from peers
    * @param ballot the one in envelope should be compatible with ballot
    */
  def findCompatibleCounterOfCommitted(nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       ballot: Ballot): P[F, CounterSet]
}
