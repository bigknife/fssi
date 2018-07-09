package fssi.ast.usecase

import bigknife.sop._
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
    * @param publickKeyData account's public key
    * @return
    */
  def signData(bytes: Array[Byte], publickKeyData: BytesValue): SP[F, BytesValue]
}
