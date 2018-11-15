package fssi.types
package biz

import fssi.base.BytesValue
import fssi.types.implicits._

/** a piece of receipt is running result and log of a transaction
  */
case class Receipt(
    transactionId: Transaction.ID,
    success: Boolean,
    logs: Vector[Receipt.Log],
    costs: Int
) extends Ordered[Receipt] {
  override def compare(that: Receipt): Int = {
    val thisId = transactionId.asBytesValue.base64
    val thatId = that.transactionId.asBytesValue.base64
    Ordering[String].compare(thisId, thatId)
  }
}

object Receipt {
  case class Log(label: String, line: String)

  def logToDeterminedBytes(log: Log): Array[Byte] = {
    Vector(log.label, log.line).mkString("\n").asBytesValue.bcBase58.getBytes
  }

  def logFromDeterminedBytes(bytes: Array[Byte]): Log = {
    new String(BytesValue.unsafeDecodeBcBase58(new String(bytes)).bytes).split("\n") match {
      case Array(label, line) => Log(label, line)
      case _ => throw new RuntimeException("insane receipt log")
    }
  }

  def logsToDeterminedBytes(log: Vector[Log]): Array[Byte] = {
    log.map(logToDeterminedBytes).map{x =>
      x.asBytesValue.bcBase58
    }.mkString("\n").getBytes("utf-8")
  }

  def logsFromDeterminedBytes(bytes: Array[Byte]): Vector[Log] = {
    new String(bytes).split("\n").toVector
      .map(x => BytesValue.unsafeDecodeBcBase58(x).bytes)
      .map(logFromDeterminedBytes)
  }

  trait Implicits {

    implicit def logToBytesValue(log: Log): Array[Byte] =
      log.label.asBytesValue.bytes ++ log.line.asBytesValue.bytes

    implicit def stackTraceElementToBytesValue(stackTraceElement: StackTraceElement): Array[Byte] =
      stackTraceElement.getFileName.asBytesValue.bytes ++
        stackTraceElement.getClassName.asBytesValue.bytes ++
        stackTraceElement.getMethodName.asBytesValue.bytes ++
        stackTraceElement.getLineNumber.asBytesValue.bytes

    implicit def receiptToBytesValue(receipt: Receipt): Array[Byte] =
      receipt.transactionId.asBytesValue.bytes ++
        receipt.success.asBytesValue.bytes ++
        receipt.logs.foldLeft(Array.emptyByteArray)((acc, n) => acc ++ n.asBytesValue.bytes) ++
        receipt.costs.asBytesValue.bytes
  }
}
