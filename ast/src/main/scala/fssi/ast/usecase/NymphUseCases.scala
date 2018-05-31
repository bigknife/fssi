package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.types._

/** use cases of Nymph */
trait NymphUseCases[F[_]] {

  /**
    * uc1. enroll, become a member of the chain who's identified by an account.
    * @param rand random string to encrypt the private key of the account being created.
    * @return account
    */
  def register(rand: String): SP[F, Account]

  /**
    * uc2. query account detail.
    * @param id account id
    * @return if the account id existed in current node or other nodes, return the account detail, or None.
    */
  def queryAccount(id: Account.ID): SP[F, Option[Account]]

  /**
    * uc3. send a transaction.
    * @param id sender's account id
    * @param transaction transaction info
    * @return return the transaction's current status
    */
  def sendTransaction(id: Account.ID, transaction: Transaction): SP[F, Transaction.Status]

  /**
    * uc4. query current status of a transaction.
    * @param id id of a transaction
    * @return if transaction existed in the chain, return current status, or None.
    */
  def queryTransactionStatus(id: Transaction.ID): SP[F, Option[Transaction.Status]]

  def randomizeTransactionID(): SP[F, Transaction.ID]

  //// some convenient functions
  /** query balance */
  def queryBalance(id: Account.ID): SP[F, Option[Token]] = queryAccount(id).map(_.map(_.balance))

  /** transfer balance */
  def transferBalance(from: Account.ID,
                      to: Account.ID,
                      amount: Token,
                      sign: String): SP[F, Transaction.Status] =
    for {
      id <- randomizeTransactionID()
      x <- sendTransaction(
        from,
        Transaction.Transfer(id, from, to, amount, Signature(sign), Transaction.Status.Pending(id)))
    } yield x

  /** publish smart contract */
  def publishContract(owner: Account.ID,
                      contract: Contract,
                      sign: String): SP[F, Transaction.Status] =
    for {

      id <- randomizeTransactionID()
      x <- sendTransaction(owner,
                           Transaction.PublishContract(id,
                                                       owner,
                                                       contract,
                                                       Signature(sign),
                                                       Transaction.Status.Pending(id)))
    } yield x

  /** invoke smart contract */
  def invokeContract(invoker: Account.ID,
                     name: Contract.Name,
                     version: Contract.Version,
                     parameter: Contract.Parameter,
                     sign: String): SP[F, Transaction.Status] =
    for {
      id <- randomizeTransactionID()
      transaction = Transaction.InvokeContract(id,
                                               invoker,
                                               name,
                                               version,
                                               parameter,
                                               Signature(sign),
                                               Transaction.Status.Pending(id))
      x <- sendTransaction(invoker, transaction)
    } yield x

}
