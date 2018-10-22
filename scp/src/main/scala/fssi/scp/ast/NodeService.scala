package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._
@sp trait NodeService[F[_]] {

  /** compute next round timeout (in ms)
    */
  def computeTimeout(round: Int): P[F, Long]

  /** check if in-nominating and no any candidate produced
    */
  def canNominateNewValue(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean]
  def cannotNominateNewValue(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean] =
    canNominateNewValue(nodeId, slotIndex).map(!_)

  /** check if nominating is stopped
    */
  def isNominatingStopped(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean]
  def isNominatingGoingOn(nodeId: NodeID, slotIndex: SlotIndex): P[F, Boolean] =
    isNominatingStopped(nodeId, slotIndex).map(!_)

  /** compute a value's hash
    */
  def hashValue(slotIndex: SlotIndex, previousValue: Value, round: Int, value: Value): P[F, Long]

  /** stop nomination process
    */
  def stopNominating(nodeId: NodeID, slotIndex: SlotIndex): P[F, Unit]

  /** do some rate-limits stuff to narrow down the nominating votes
    */
  def narrowDownVotes(nodeId: NodeID,
                      slotIndex: SlotIndex,
                      values: ValueSet,
                      previousValue: Value): P[F, ValueSet]

  /** create nomination message based on local state
    */
  def createNominationMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.Nomination]

  /** create ballot message based on local state
    */
  def createBallotMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.BallotMessage]

  /** make a envelope for a message
    */
  def putInEnvelope[M <: Message](nodeId: NodeID, message: M): P[F, Envelope[M]]

  /** broadcast message envelope
    */
  def broadcastEnvelope[M <: Message](nodeId: NodeID, envelope: Envelope[M]): P[F, Unit]

  /** verify the signature of the envelope
    */
  def isSignatureVerified[M <: Message](envelope: Envelope[M]): P[F, Boolean]
  def isSignatureTampered[M <: Message](envelope: Envelope[M]): P[F, Boolean] =
    isSignatureVerified(envelope).map(!_)

  /** check the statement to see if it is illegal
    */
  def isStatementValid[M <: Message](statement: Statement[M]): P[F, Boolean]
  def isStatementInvalid[M <: Message](statement: Statement[M]): P[F, Boolean] =
    isStatementValid(statement).map(!_)

  /** check the message to see if it's sane
    */
  def isMessageSane(message: Message): P[F, Boolean]
  def isMessageNotSane(message: Message): P[F, Boolean] =
    isMessageSane(message).map(!_)

  /** check a node set to see if they can construct a quorum for a node (configured quorum slices)
    */
  def isQuorum(nodeId: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** check a node set to see if they can construct a vblocking set for a node (configured quorum slices)
    */
  def isVBlocking(nodeId: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** check current phase to see if a message can be ignored
    */
  def needHandleMessage(nodeId: NodeID, slotIndex: SlotIndex, message: Message): P[F, Boolean]

  /** check ballot can be commit (vote (commit b))
    */
  def canBallotCommitted(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean]
  def cannotBallotCommitted(nodeId: NodeID, slotIndex: SlotIndex, ballot: Ballot): P[F, Boolean] =
    canBallotCommitted(nodeId, slotIndex, ballot).map(!_)
}
