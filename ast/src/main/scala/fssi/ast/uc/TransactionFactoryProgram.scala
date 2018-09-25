package fssi
package ast
package uc

import fssi.types.biz._
import fssi.types.base._

import bigknife.sop._
import bigknife.sop.implicits._
import types.implicits._

import java.io.File

/** transaction factory.
  * generate three types of Transaction
  */
trait TransactionFactoryProgram[F[_]] extends BaseProgram[F] {
  import model._

  /** create transfer transaction
    */
  def createTransferTransaction(accountFile: File,
                                secretKeyFile: File,
                                payee: Account.ID,
                                token: Token): SP[F, Transaction.Transfer] = {
    import accountStore._
    import accountService._
    import transactionService._
    for {
      account        <- loadAccountFromFile(accountFile).right
      secretKey      <- loadAccountSecretKeyFile(secretKeyFile).right
      privKey        <- aesDecryptPrivKey(account.encPrivKey, secretKey, account.iv).right
      id             <- createTransactionID(account.id)
      transfer       <- createTransfer(id, payer = account.id, payee, token)
      signedTransfer <- signTransfer(transfer, privKey)
    } yield signedTransfer
  }

  /** create a deploy transaction
    */
  def createDeployTransaction(accountFile: File,
                              secretKeyFile: File,
                              contractFile: File): SP[F, Transaction.Deploy] = {

    import accountStore._
    import accountService._
    import transactionService._
    import contractStore._
    for {
      account      <- loadAccountFromFile(accountFile).right
      secretKey    <- loadAccountSecretKeyFile(secretKeyFile).right
      privKey      <- aesDecryptPrivKey(account.encPrivKey, secretKey, account.iv).right
      id           <- createTransactionID(account.id)
      userContract <- loadUserContract(contractFile).right
      deploy       <- createDeploy(id, owner = account.id, userContract)
      signedDeploy <- signDeploy(deploy, privKey)
    } yield signedDeploy
  }

  /** create a run(user contract) transaction
    */
  def createRunTransaction(accountFile: File,
                           secretKeyFile: File,
                           contractName: UniqueName,
                           contractVersion: Contract.Version,
                           methodAlias: String,
                           parameter: Option[Contract.UserContract.Parameter]): SP[F, Transaction.Run] = {
    import accountStore._
    import accountService._
    import transactionService._
    import contractStore._

    for {
      account      <- loadAccountFromFile(accountFile).right
      secretKey    <- loadAccountSecretKeyFile(secretKeyFile).right
      privKey      <- aesDecryptPrivKey(account.encPrivKey, secretKey, account.iv).right
      id           <- createTransactionID(account.id)
      run       <- createRun(id, caller = account.id, contractName, contractVersion, methodAlias, parameter)
      signedRun <- signRun(run, privKey)
    } yield signedRun
  }
}

object TransactionFactoryProgram {
  def apply[F[_]](implicit M: components.Model[F]): TransactionFactoryProgram[F] =
    new TransactionFactoryProgram[F] {
      val model: components.Model[F] = M
    }
}
