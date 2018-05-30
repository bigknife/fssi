package fssi.ast.domain

import bigknife.sop._
import macros._
import implicits._
import fssi.ast.domain.types._

/** network service */
@sp trait NetworkService[F[_]] {
  /** find current node*/
  def currentNode(): P[F, Node]

  /** find warrior nodes of a nymph node */
  def warriorNodesOfNymph(nymphNode: Node): P[F, Vector[Node]]

  /** disseminate a network message */
  def disseminate(packet: DataPacket, nodes: Vector[Node]): P[F, Unit]

  /** build a network message by using an account */
  def buildCreateAccountDataMessage(account: Account): P[F, DataPacket]

  /** build a ACCOUNT-SYNC message */
  def buildSyncAccountMessage(id: Account.ID): P[F, DataPacket]

  /** build s SUBMIT-TRANSACTION message */
  def buildSubmitTransactionMessage(account: Account, transaction: Transaction): P[F, DataPacket]

  /** build a TRANSACTION-SYNC message */
  def buildSyncTransactionMessage(): P[F, DataPacket]
}
