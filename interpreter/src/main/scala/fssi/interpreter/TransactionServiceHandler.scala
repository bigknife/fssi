package fssi
package interpreter

import types.biz._
import types.base._
import ast._
import utils._

class TransactionServiceHandler
    extends TransactionService.Handler[Stack]
    with BlockCalSupport
    with LogSupport {

  /** create a transaction id
    */
  override def createTransactionID(accountId: Account.ID): Stack[Transaction.ID] = Stack {
    // create a unique id
    val bytes = java.util.UUID.randomUUID().toString.getBytes ++ accountId.value
    Transaction.ID(crypto.hash(bytes))
  }

  /** create a transfer object with an empty signature field
    */
  override def createTransfer(id: Transaction.ID,
                              payer: Account.ID,
                              payee: Account.ID,
                              token: Token): Stack[Transaction.Transfer] = Stack {

    Transaction.Transfer(id, payer, payee, token, Signature.empty, System.currentTimeMillis)
  }

  /** create a publish-contract transaction object with an empty signature field
    */
  override def createDeploy(id: Transaction.ID,
                            owner: Account.ID,
                            contract: Contract.UserContract): Stack[Transaction.Deploy] = Stack {
    Transaction.Deploy(
      id,
      owner,
      contract,
      Signature.empty,
      System.currentTimeMillis
    )
  }

  /** create run-contract transaction object with an empty signature field
    */
  override def createRun(
      id: Transaction.ID,
      caller: Account.ID,
      contractName: UniqueName,
      version: Contract.Version,
      methodAlias: String,
      parameter: Option[Contract.UserContract.Parameter]): Stack[Transaction.Run] = Stack {
    Transaction.Run(
      id,
      caller,
      contractName,
      version,
      methodAlias,
      parameter,
      Signature.empty,
      System.currentTimeMillis
    )
  }

  /** create a transfer object with an empty signature field
    */
  /*
  override def createUnsignedTransfer(payer: Account.ID,
                                      payee: Account.ID,
                                      token: Token): Stack[Transaction.Transfer] = Stack {
    setting =>
      val randomId = Transaction.ID(
        java.util.UUID.randomUUID.toString
      )
      Transaction.Transfer(
        randomId,
        payer,
        payee,
        token,
        Signature.empty,
        System.currentTimeMillis
      )
  }
   */

  /*
  override def createUnsignedRunContractTransaction(
      invoker: Account.ID,
      contractName: UniqueName,
      contractVersion: Version,
      method: Contract.Method,
      parameter: Contract.Parameter): Stack[Transaction.RunContract] = Stack { setting =>
    val randomId = Transaction.ID(
      java.util.UUID.randomUUID.toString
    )

    Transaction.RunContract(
      randomId,
      invoker,
      contractName,
      contractVersion,
      method,
      parameter,
      Signature.empty,
      System.currentTimeMillis
    )
  }
   */

  /** create a publish-contract transaction object with an empty signature field
    */
  /*
  override def createUnsignedPublishContractTransaction(
      owner: Account.ID,
      contract: Contract.UserContract): Stack[Transaction.PublishContract] = Stack { setting =>
    val randomId = Transaction.ID(
      java.util.UUID.randomUUID.toString
    )
    Transaction.PublishContract(
      randomId,
      owner,
      contract,
      Signature.empty,
      System.currentTimeMillis
    )
  }
   */

  /** calculate bytes of the transaction object which will be signed
    */
  /*
  override def calculateSingedBytesOfTransaction(transaction: Transaction): Stack[BytesValue] =
    Stack { setting =>
      calculateBytesToBeSignedOfTransaction(transaction)
    }
 */
}

object TransactionServiceHandler {
  private val instance = new TransactionServiceHandler

  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler = instance
  }
}
