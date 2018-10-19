package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._

@sp trait NodeStore[F[_]] {
  /** get current nominating round
    */
  def currentNominateRound(nodeId: NodeID, slotIndex: SlotIndex): P[F, Int]

  /** set nominate round to the next one
    */
  def gotoNextNominateRound(nodeId: NodeID, slotIndex: SlotIndex): P[F, Unit]

  /** save new values to current voted nominations
    */
  def voteNewNominations(nodeId: NodeID, slotIndex: SlotIndex, newVotes: ValueSet): P[F, Unit]

  /** save new values to current accepted nominations
    */
  def acceptNewNominations(nodeId: NodeID, slotIndex: SlotIndex, values: ValueSet): P[F, Unit]

  /** find current un-accepted votes values (vote(nominate x))
    */
  def unAcceptedNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]

  /** nominate a value as candidate value
    */
  def candidateNewValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Unit]

  /** find current accepted nomination votes
    */
  def acceptedNominations(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]

  /** the set of nodes which have voted(nominate x)
    */
  def nodesVotedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** the set of nodes which have accept(nominate x)
    */
  def nodesAcceptedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** the set of nodes which have vote(prepare b)
    */
  def nodesVotedPrepare(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** the set of nodes which have accepted(prepare b)
    */
  def nodesAcceptedPrepare(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Set[NodeID]]

  /** save latest message sent from any node. SHOULD saved by different message type.
    */
  def saveLatestStatement[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, statement: Statement[M]): P[F, Unit]

  /** compare the message with local cached message 
    */
  def isStatementNewer[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, statement: Statement[M]): P[F, Boolean]

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

  /** check if a envelope can be emitted
    */
  def canEmit[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, envelope: Envelope[M]): P[F, Boolean]
}
