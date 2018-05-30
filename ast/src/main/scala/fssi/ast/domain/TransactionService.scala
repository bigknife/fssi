package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.ast.domain.types.{Token, Transaction}

@sp trait TransactionService[F[_]] {
  /** create a randomized transaction id */
  def randomTransactionID(): P[F, Transaction.ID]

  /** compute the cost of a transaction */
  def instrumentCost(transaction: Transaction): P[F, Token]
}
