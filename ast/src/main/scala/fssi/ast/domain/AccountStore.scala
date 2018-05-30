package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

/** account store */
@sp trait AccountStore[F[_]] {

  /** save an account which is not active */
  def saveInactiveAccount(account: Account): P[F, Account]

  /** find account with an id */
  def findAccount(id: Account.ID): P[F, Option[Account]]
}
