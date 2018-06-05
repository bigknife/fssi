package fssi.ast.domain.types

case class TransactionSendingStatus(tip: String, status: Transaction.Status)

object TransactionSendingStatus {
  def reject(id: Transaction.ID, t: Throwable): TransactionSendingStatus =
    TransactionSendingStatus(t.getMessage, Transaction.Status.Rejected(id))

  def pending(id: Transaction.ID): TransactionSendingStatus =
    TransactionSendingStatus("Accepted", Transaction.Status.Pending(id))
}
