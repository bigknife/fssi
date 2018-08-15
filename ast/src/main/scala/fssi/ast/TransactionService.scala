package fssi
package ast

import types._
import utils._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait TransactionService[F[_]] {
  /** create a transfer object with an empty signature field
    */
  def createUnsignedTransfer(payer: Account.ID, payee: Account.ID, token: Token): P[F, Transaction.Transfer]

  /** calculate bytes of the transfer object which will be signed
    */
  def toBeSingedBytesOfTransfer(transfer: Transaction.Transfer): P[F, BytesValue]
}
