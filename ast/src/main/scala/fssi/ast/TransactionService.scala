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

  /** calculate bytes of the transfer object which will be signed
    */
  def calculateSingedBytesOfTransfer(transfer: Transaction.Transfer): P[F, BytesValue]

  /** calculate bytes of the PublishContract object which will be signed
    */
  def calculateSingedBytesOfPublishContract(
      publishContract: Transaction.PublishContract): P[F, BytesValue]

  /** calculate bytes of the RunContract object which will be signed
    */
  def calculateSingedBytesOfRunContract(runContract: Transaction.RunContract): P[F, BytesValue]

  /** calculate bytes of the transaction object which will be signed
    */
  def calculateSingedBytesOfTransaction(transaction: Transaction): P[F, BytesValue] =
    transaction match {
      case x: Transaction.Transfer        => calculateSingedBytesOfTransfer(x)
      case x: Transaction.PublishContract => calculateSingedBytesOfPublishContract(x)
      case x: Transaction.RunContract     => calculateSingedBytesOfRunContract(x)
    }
}
