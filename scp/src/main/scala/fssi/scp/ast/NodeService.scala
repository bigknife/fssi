package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._
@sp trait NodeService[F[_]] {

  def cacheNodeQuorumSet(nodeId: NodeID, quorumSet: QuorumSet): P[F, Unit]

  /** compute next round timeout (in ms)
    *
    * @see SCPDriver.cpp#79
    */
  def computeTimeout(round: Int): P[F, Long]

  /** check if in-nominating and no any candidate produced
    *
    * @see NominationProtocl.cpp#465-469
    */
  def canNominateNewValue(slotIndex: SlotIndex, timeout: Boolean): P[F, Boolean]
  def cannotNominateNewValue(slotIndex: SlotIndex, timeout: Boolean): P[F, Boolean] =
    canNominateNewValue(slotIndex, timeout).map(!_)

  /** check if nominating is stopped
    */
  def isNominatingStopped(slotIndex: SlotIndex): P[F, Boolean]

  /** compute a value's hash
    */
  def hashValue(slotIndex: SlotIndex, previousValue: Value, round: Int, value: Value): P[F, Long]

  /** start nomination process, set nominationStarted to true
    */
  def startNominating(slotIndex: SlotIndex): P[F, Unit]

  /** stop nomination process, set nominationStarted to false
    */
  def stopNominating(slotIndex: SlotIndex): P[F, Unit]

  /** do some rate-limits stuff to narrow down the nominating votes
    *
    * @see NominationProtocl.cpp#476-506
    */
  def updateAndGetNominateLeaders(slotIndex: SlotIndex, previousValue: Value): P[F, Set[NodeID]]

  /** create nomination message based on local state
    */
  def createNominationMessage(slotIndex: SlotIndex): P[F, Message.Nomination]

  /** create ballot message based on local state
    */
  def createBallotMessage(slotIndex: SlotIndex): P[F, Message.BallotMessage]

  /** make a envelope for a message
    */
  def putInEnvelope[M <: Message](slotIndex: SlotIndex, message: M): P[F, Envelope[M]]

  /** verify the signature of the envelope
    */
  def isSignatureVerified[M <: Message](envelope: Envelope[M]): P[F, Boolean]
  def isSignatureTampered[M <: Message](envelope: Envelope[M]): P[F, Boolean] =
    isSignatureVerified(envelope).map(!_)

  /** check the statement to see if it is illegal
    */
  def isStatementValid[M <: Message](nodeId: NodeID,
                                     slotIndex: SlotIndex,
                                     statement: Statement[M]): P[F, Boolean]
  def isStatementInvalid[M <: Message](nodeId: NodeID,
                                       slotIndex: SlotIndex,
                                       statement: Statement[M]): P[F, Boolean] =
    isStatementValid(nodeId, slotIndex, statement).map(!_)

  /** check a node set to see if they can construct a quorum for a node (configured quorum slices)
    */
  def isLocalQuorum(nodes: Set[NodeID]): P[F, Boolean]
  def isQuorum(nodeID: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** check a node set to see if they can construct a vblocking set for a node (configured quorum slices)
    */
  def isLocalVBlocking(nodes: Set[NodeID]): P[F, Boolean]
  def isVBlocking(nodeID: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** get values from a ballot message
    */
  def valuesFromBallotMessage(msg: Message.BallotMessage): P[F, ValueSet]

  /** check a ballot can be used as a prepared candidate based on local p , p' and phase
    */
  def canBallotBePrepared(slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean]
  def ballotCannotBePrepared(slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean] =
    canBallotBePrepared(slotIndex, ballot).map(!_)

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    *
    * @see BallotProtocol.cpp#937-938
    */
  def canBallotBeHighestCommitPotentially(slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean]

  /** check a ballot can be potentially raise h, be confirmed prepared, to a commit
    *
    * @see BallotProtocol.cpp#970-973, 975-978 b should be compatible with newH
    */
  def canBallotBeLowestCommitPotentially(slotIndex: SlotIndex,
                                         b: Ballot,
                                         newH: Ballot): P[F, Boolean]

  /** check if it's necessary to set `c` based on a new `h`
    *
    * @see BallotProtocol.cpp#961
    */
  def needSetLowestCommitBallotUnderHigh(slotIndex: SlotIndex, high: Ballot): P[F, Boolean]
  def notNecessarySetLowestCommitBallotUnderHigh(slotIndex: SlotIndex,
                                                 high: Ballot): P[F, Boolean] =
    needSetLowestCommitBallotUnderHigh(slotIndex, high).map(!_)
}
