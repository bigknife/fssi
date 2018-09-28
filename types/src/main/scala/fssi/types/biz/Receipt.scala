package fssi.types
package biz

import base._

/** a piece of receipt is running result and log of a transaction
  */
case class Receipt(
  transactionId: Transaction.ID,
  success: Boolean,
  exception: Option[Vector[StackTraceElement]],
  logs: Vector[Receipt.Log],
  costs: Int
)

object Receipt {
  case class Log(label: String, line: String)
}
