package fssi.ast.domain

import bigknife.sop._
import macros._
import implicits._
import fssi.ast.domain.types._

/** network service */
@sp trait NetworkService[F[_]] {

  /** find warrior nodes of a nymph node */
  def warriorNodesOfNymph(nymphNode: Node): P[F, Vector[Node.Address]]

  /** disseminate a network message */
  def disseminate(packet: DataPacket, nodes: Vector[Node.Address]): P[F, Unit]

  /** build a network message by using an account */
  def buildCreateAccountDataMessage(account: Account): P[F, DataPacket]

  /** build a ACCOUNT-SYNC message */
  def buildSyncAccountMessage(id: Account.ID): P[F, DataPacket]

  /** build s SUBMIT-TRANSACTION message */
  def buildSubmitTransactionMessage(account: Account, transaction: Transaction): P[F, DataPacket]

  /** build a TRANSACTION-SYNC message */
  def buildSyncTransactionMessage(): P[F, DataPacket]

  /** startup a p2p node, return runtime node.
    */
  def startupP2PNode(node: Node): P[F, Node]

  /** shutdown p2p node
    *
    */
  def shutdownP2PNode(): P[F, Unit]

  /** create a node
    * @param seeds seed nodes, format: 'ip:port'
    */
  def createNode(port: Int, ip: String, seeds: Vector[String]): P[F, Node]
}
