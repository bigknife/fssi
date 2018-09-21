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
}

object TransactionFactoryProgram {
  def apply[F[_]](implicit M: components.Model[F]): TransactionFactoryProgram[F] =
    new TransactionFactoryProgram[F] {
      val model: components.Model[F] = M
    }
}
