package fssi
package ast

import types._, exception._
import utils._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait TransactionService[F[_]] {

  /** create a transfer object with an empty signature field
    */
  def createUnsignedTransfer(payer: Account.ID,
                             payee: Account.ID,
                             token: Token): P[F, Transaction.Transfer]

  /** calculate bytes of the transaction object which will be signed
    */
  def calculateSingedBytesOfTransaction(transaction: Transaction): P[F, BytesValue]

}
