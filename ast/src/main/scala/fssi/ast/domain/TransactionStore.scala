package fssi.ast.domain

import bigknife.sop._
import fssi.ast.domain.types.Transaction
import macros._
import implicits._

@sp trait TransactionStore[F[_]] {
  /** find transaction info from local store */
  def findTransaction(id: Transaction.ID): P[F, Option[Transaction]]
}
