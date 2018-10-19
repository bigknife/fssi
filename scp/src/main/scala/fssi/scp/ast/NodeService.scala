package fssi.scp
package ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import scala.collection.immutable._

import types._


@sp trait NodeService[F[_]] {

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

  /** do some rate-limits stuff to narrow down the nominating votes
    */
  def narrowDownVotes(nodeId: NodeID,
                      slotIndex: SlotIndex,
                      values: ValueSet,
                      previousValue: Value): P[F, ValueSet]

  /** make a envelope for a message
    */
  def putInEnvelope[M <: Message](nodeId: NodeID, message: M): P[F, Envelope[M]]

  /** broadcast message envelope
    */
  def broadcastEnvelope[M <: Message](nodeId: NodeID, envelope: Envelope[M]): P[F, Unit]

  /** verify the signature of the envelope
    */
  def verifySignature[M <: Message](envelope: Envelope[M]): P[F, Boolean]

  /** check the statement to see if it is illegal
    */
  def checkStatementValidity[M <: Message](statement: Statement[M]): P[F, Boolean]

  /** check a node set to see if they can construct a quorum for a node (configured quorum slices)
    */
  def isQuorum(nodeId: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** check a node set to see if they can construct a vblocking set for a node (configured quorum slices)
    */
  def isVBlocking(nodeId: NodeID, nodes: Set[NodeID]): P[F, Boolean]

  /** create a vote-nomination message based on current node state
    */
  def createVoteNominationMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.VoteNominations]

  /** create a accept-nomination message based on current node state
    */
  def createAcceptNominationMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.AcceptNominations]

  /** create a vote-prepare-ballot message based on current node state
    */
  def createVotePrepareMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.VotePrepare]

  /** create a accept-prepare-ballot message based on current node state
    */
  def createAcceptPrepareMessage(nodeId: NodeID, slotIndex: SlotIndex): P[F, Message.AcceptPrepare]

  /** check the message to see if it's sane
    */
  def isMessageSane(message: Message): P[F, Boolean]
  def isMessageNotSane(message: Message): P[F, Boolean] =
    isMessageSane(message).map(!_)

  /** check current phase to see if a message can be ignored
    */
  def needHandleMessage(nodeId: NodeID, slotIndex: SlotIndex, message: Message): P[F, Boolean]

}
