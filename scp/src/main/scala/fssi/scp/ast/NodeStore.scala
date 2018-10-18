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

  /** save new values to current voted nominations
    */
  def voteNewNominations(nodeId: NodeID, slotIndex: SlotIndex, newVotes: ValueSet): P[F, Unit]

  /** save new values to current accepted nominations
    */
  def acceptNewNominations(nodeId: NodeID, slotIndex: SlotIndex, values: ValueSet): P[F, Unit]

  /** find current un-accepted votes values (vote(nominate x))
    */
  def unAcceptedVotes(nodeId: NodeID, slotIndex: SlotIndex): P[F, ValueSet]

  /** the set of nodes which has voted(nominate x)
    */
  def nodesVotedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** the set of nodes which has accept(nominate x)
    */
  def nodesAcceptedNomination(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Set[NodeID]]

  /** save latest message sent from any node. SHOULD saved by diffent message type.
    */
  def saveLatestStatement[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, statement: Statement[M]): P[F, Unit]

  /** compare the message with local cached message 
    */
  def isStatementNewer[M <: Message](nodeId: NodeID, slotIndex: SlotIndex, statement: Statement[M]): P[F, Boolean]

  /** check if a value has been stored as votes or accepted.
    */
  def valueVotedOrAccepted(nodeId: NodeID, slotIndex: SlotIndex, value: Value): P[F, Boolean]
}
