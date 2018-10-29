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
                                token: Token): SP[F, Transaction.Transfer] = ???

  /** create a deploy transaction
    */
  def createDeployTransaction(accountFile: File,
                              secretKeyFile: File,
                              contractFile: File): SP[F, Transaction.Deploy] = ???

  /** create a run(user contract) transaction
    */
  def createRunTransaction(
      accountFile: File,
      secretKeyFile: File,
      contractName: UniqueName,
      contractVersion: Contract.Version,
      methodAlias: String,
      parameter: Option[Contract.UserContract.Parameter]): SP[F, Transaction.Run] = ???
}
