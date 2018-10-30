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

  trait Implicits {
    import fssi.types.implicits._

    implicit def logToBytesValue(log: Log): Array[Byte] =
      log.label.asBytesValue.bytes ++ log.line.asBytesValue.bytes

    implicit def stackTraceElementToBytesValue(stackTraceElement: StackTraceElement): Array[Byte] =
      stackTraceElement.getFileName.asBytesValue.bytes ++ stackTraceElement.getClassName.asBytesValue.bytes ++ stackTraceElement.getMethodName.asBytesValue.bytes ++ stackTraceElement.getLineNumber.asBytesValue.bytes

    implicit def receiptToBytesValue(receipt: Receipt): Array[Byte] =
      receipt.transactionId.asBytesValue.bytes ++ receipt.success.asBytesValue.bytes ++ receipt.exception
        .map(_.foldLeft(Array.emptyByteArray)((acc, n) => acc ++ n.asBytesValue.bytes))
        .getOrElse(Array.emptyByteArray) ++ receipt.logs.foldLeft(Array.emptyByteArray)((acc, n) =>
        acc ++ n.asBytesValue.bytes) ++ receipt.costs.asBytesValue.bytes
  }
}
