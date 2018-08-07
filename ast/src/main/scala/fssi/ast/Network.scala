package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

/** P2P network service
  */
@sp trait Network[F[_]] {
  /** startup p2p node
    */
  def startupP2PNode(handler: JsonMessageHandler): P[F, Node]

  /** shutdown current node
    */
  def shutdownP2PNode(node: Node): P[F, Unit]

  /** send json message to some nodes
    */
  def broadcastMessage(message: JsonMessage): P[F, Unit]

  /** bind an account to a node, then all actions will
    * be considered to be acted by this account
    */
  def bindAccount(node: Node): P[F, Node]
}
