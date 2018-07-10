package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Node
import fssi.ast.domain.types.{Proposal, _}

trait WarriorUseCases[F[_]] extends P2PUseCases[F]{
  /**
    * uc1. handle message from Nymph
    *     transaction -> contract -> moment
    */
  def processTransaction(transaction: Transaction): SP[F, Transaction.Status]

  /**
    * uc2. run consensus when the proposal pool is full or time is up.
    */
  def validateProposal(): SP[F, Unit]

  /**
    * uc3. handle the message of CreateAccount
    * @param account account created in Nymph, or heard from other warriors
    * @return
    */
  def createNewAccount(account: Account): SP[F, Unit]

  /**
    * use account's public key to sign a data block
    * @param bytes data
    * @param publicKeyData account's public key
    * @return
    */
  def signData(bytes: Array[Byte], publicKeyData: BytesValue): SP[F, BytesValue]

  /**
    * broadcast message to peers
    * @param message message
    * @return
    */
  def broadcastMessage(message: DataPacket): SP[F, Unit]

  /**
    * query current warrior node info
    * @return
    */
  def currentNode(): SP[F, Node]

  /**
    * verify data's sign
    * @param source source bytes
    * @param publicKeyData public key
    * @return
    */
  def verifySign(source: Array[Byte], signature: Array[Byte], publicKeyData: Array[Byte]): SP[F, Boolean]

  /**
    * after consensus engine run, moments have reached agreement, so we can persist them
    * @param height the new block length to be persisted
    * @param moments all moments
    * @return
    */
  def momentsDetermined(moments: Vector[Moment], height: BigInt): SP[F, Unit]
}
