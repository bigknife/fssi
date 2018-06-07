package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.ast.domain.types.{Contract, Token, Transaction}

@sp trait TransactionService[F[_]] {

  /** create a randomized transaction id */
  def randomTransactionID(): P[F, Transaction.ID]

  /**
    * create a transfer transaction, the sign is not set.
    * @param from from account
    * @param to to account
    * @param amount token amount, with `Sweet` unit.
    * @return
    */
  def createTransferWithoutSign(id: Transaction.ID,
                                from: String,
                                to: String,
                                amount: Long): P[F, Transaction.Transfer]

  /**
    * create a publish contract transaction
    * @param id trans id
    * @param accountId account id of the contract owner.
    * @param name name of contract
    * @param version version of contract
    * @param contract contract data
    * @return
    */
  def createPublishContractWithoutSign(id: Transaction.ID,
                                       accountId: String,
                                       name: String,
                                       version: String,
                                       contract: Contract): P[F, Transaction.PublishContract]

  /**
    * create a run contract transaction
    * @param id trans id
    * @param accountId account id of the contract owner
    * @param name name of contract
    * @param version version of contract
    * @param params parameters
    * @return
    */
  def createRunContractWithoutSign(id: Transaction.ID,
                                   accountId: String,
                                   name: String,
                                   version: String,
                                   params: Contract.Parameter): P[F, Transaction.InvokeContract]
}
