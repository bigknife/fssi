package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.types.Account

/** command line tools */
trait CmdToolUseCases[F[_]] {

  /** use rand to create an account
    * the account info won't enter into the chain.
    */
  def createAccount(rand: String): SP[F, Account]
}
