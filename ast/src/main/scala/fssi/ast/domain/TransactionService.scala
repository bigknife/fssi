package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.ast.domain.types._
import fssi.contract.States

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
  def createPublishContractWithoutSign(
      id: Transaction.ID,
      accountId: String,
      name: String,
      version: String,
      contract: Contract.UserContract): P[F, Transaction.PublishContract]

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
                                   function: String,
                                   params: Contract.Parameter): P[F, Transaction.InvokeContract]

  /**
    * calculate the bytes used to be signed of states
    * @param states States object
    * @return bytes value
    */
  def calculateStatesToBeSigned(states: States): P[F, BytesValue]

  /**
    * create a moment via transaction and states changes.
    * @param transaction
    * @param statesChange
    * @return
    */
  def createMoment(transaction: Transaction,
                   statesChange: StatesChange,
                   oldStatesHash: BytesValue,
                   newStatesHash: BytesValue): P[F, Moment]
}
