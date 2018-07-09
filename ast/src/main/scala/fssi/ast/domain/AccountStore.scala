package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.ast.domain.types.Account

@sp trait AccountStore[F[_]] {
  def currentAccount(): P[F, Option[Account]]
}
