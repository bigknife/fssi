package fssi
package ast

import types.exception._
import types.biz._
import types.base._
import utils._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait TransactionService[F[_]] {

  /** create a transaction id
    */
  def createTransactionID(accountId: Account.ID): P[F, Transaction.ID]

  /** create a transfer object with an empty signature field
    */
  def createTransfer(id: Transaction.ID,
                     payer: Account.ID,
                     payee: Account.ID,
                     token: Token): P[F, Transaction.Transfer]

  /** create a publish-contract transaction object with an empty signature field
    */
  def createDeploy(id: Transaction.ID,
                   owner: Account.ID,
                   contract: Contract.UserContract): P[F, Transaction.Deploy]

  /** create run-contract transaction object with an empty signature field
    */
  def createRun(id: Transaction.ID,
                caller: Account.ID,
                contractName: UniqueName,
                version: Contract.Version,
                methodAlias: String,
                parameter: Option[Contract.UserContract.Parameter]): P[F, Transaction.Run]

  /** Create a publish-contract transaction object with an empty signature field
    */
  /*
  def createUnsignedPublishContractTransaction(
      owner: Account.ID,
      contract: Contract.UserContract): P[F, Transaction.PublishContract]
   */

  /** create a run-contract transaction object with an empty signature field
    */
  /*
  def createUnsignedRunContractTransaction(
      invoker: Account.ID,
      contractName: UniqueName,
      contractVersion: Version,
      method: Contract.Method,
      parameter: Contract.Parameter): P[F, Transaction.RunContract]
   */

  /** calculate bytes of the transaction object which will be signed
    */
  /*
  def calculateSingedBytesOfTransaction(transaction: Transaction): P[F, BytesValue]
 */

}
