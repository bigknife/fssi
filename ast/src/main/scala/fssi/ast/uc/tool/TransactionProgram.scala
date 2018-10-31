package fssi.ast.uc
package tool

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait TransactionProgram[F[_]] extends ToolProgram[F] with BaseProgram[F] {
  import model._

  /** create transfer transaction
    */
  def createTransferTransaction(accountFile: File,
                                secretKeyFile: File,
                                payee: Account.ID,
                                token: Token): SP[F, Transaction.Transfer] = {
    for {
      accountOrFailed   <- store.loadAccountFromFile(accountFile)
      account           <- err.either(accountOrFailed)
      secretKeyOrFailed <- store.loadSecretKeyFromFile(secretKeyFile)
      secretKey         <- err.either(secretKeyOrFailed)
      privateKey        <- crypto.decryptAccountPrivKey(account.encPrivKey, secretKey, account.iv)
      transactionId     <- contract.generateTransactionID()
      transfer          <- contract.createTransferTransaction(transactionId, account.id, payee, token)
      transferSignature <- crypto.makeTransactionSignature(transfer, privateKey)
      signedTransfer = transfer.copy(signature = transferSignature)
    } yield signedTransfer
  }

  /** create a deploy transaction
    */
  def createDeployTransaction(accountFile: File,
                              secretKeyFile: File,
                              contractFile: File): SP[F, Transaction.Deploy] = {
    for {
      accountOrFailed      <- store.loadAccountFromFile(accountFile)
      account              <- err.either(accountOrFailed)
      secretKeyOrFailed    <- store.loadSecretKeyFromFile(secretKeyFile)
      secretKey            <- err.either(secretKeyOrFailed)
      privateKey           <- crypto.decryptAccountPrivKey(account.encPrivKey, secretKey, account.iv)
      userContractOrFailed <- contract.loadContractFromFile(account.pubKey, contractFile)
      userContract         <- err.either(userContractOrFailed)
      contractSignature    <- crypto.makeContractSignature(userContract, privateKey)
      signedContract = userContract.copy(signature = contractSignature)
      transactionId   <- contract.generateTransactionID()
      deploy          <- contract.createDeployTransaction(transactionId, account.id, signedContract)
      deploySignature <- crypto.makeTransactionSignature(deploy, privateKey)
      signedDeploy = deploy.copy(signature = deploySignature)
    } yield signedDeploy
  }

  /** create a run(user contract) transaction
    */
  def createRunTransaction(
      accountFile: File,
      secretKeyFile: File,
      contractName: UniqueName,
      contractVersion: Contract.Version,
      methodAlias: String,
      parameter: Option[Contract.UserContract.Parameter]): SP[F, Transaction.Run] = {
    for {
      accountOrFailed   <- store.loadAccountFromFile(accountFile)
      account           <- err.either(accountOrFailed)
      secretKeyOrFailed <- store.loadSecretKeyFromFile(secretKeyFile)
      secretKey         <- err.either(secretKeyOrFailed)
      privateKey        <- crypto.decryptAccountPrivKey(account.encPrivKey, secretKey, account.iv)
      transactionId     <- contract.generateTransactionID()
      run <- contract.createRunTransaction(transactionId,
                                           account.id,
                                           contractName,
                                           contractVersion,
                                           methodAlias,
                                           parameter)
      runSignature <- crypto.makeTransactionSignature(run, privateKey)
      signedRun = run.copy(signature = runSignature)
    } yield signedRun
  }
}
