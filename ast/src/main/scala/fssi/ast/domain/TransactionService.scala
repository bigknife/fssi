package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.ast.domain.types.{Token, Transaction}

@sp trait TransactionService[F[_]] {
  /** create a randomized transaction id */
  def randomTransactionID(): P[F, Transaction.ID]

  /**
    * create a transfer transaction, the sign is not set.
    * @param from from account
    * @param to to account
    * @param amount token amount, with `Sweet` unit.
    * @return
    */
  def createTransferWithoutSign(id: Transaction.ID, from: String, to: String, amount: Long): P[F, Transaction.Transfer]
}
