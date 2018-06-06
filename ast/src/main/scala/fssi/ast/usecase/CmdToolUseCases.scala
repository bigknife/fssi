package fssi.ast.usecase

import java.nio.file.Path

import bigknife.sop._
import fssi.ast.domain.types._


/** command line tools */
trait CmdToolUseCases[F[_]] {

  /** use rand to create an account
    * the account info won't enter into the chain.
    */
  def createAccount(rand: String): SP[F, Account]

  /** create a transfer transaction */
  def createTransfer(from: String,
                     to: String,
                     amount: Long,
                     privateKey: String,
                     password: String,
                     iv: String): SP[F, Transaction]

  /** compile a contract project, output jar file */
  def compileContract(source: Path): SP[F, BytesValue]
}
