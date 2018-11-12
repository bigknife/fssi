package fssi.interpreter.scp

import fssi.scp.types._
import fssi.store.mpt.Hash
import fssi.types.TransactionSet
import fssi.types.implicits._
import fssi.store.implicits._

case class BlockValue(
    height: BigInt,
    previousBlockHash: Hash,
    currentBlockHash: Hash,
    previousStateHash: Hash,
    currentStateHash: Hash,
    timestamp: Timestamp,
    transactions: TransactionSet
) extends Value {

  override def rawBytes: Array[Byte] = {
    // height + previousBlockHash + currentBlockHash +
    // previousStateHash + currentStateHash + timestamp + transactions
    val transactionBytes = transactions.foldLeft(Array.emptyByteArray) { (acc, n) =>
      acc ++ n.asBytesValue.bytes
    }

    (height.asBytesValue.any ++ previousBlockHash.asBytesValue.any ++
      currentBlockHash.asBytesValue.any ++ previousStateHash.asBytesValue.any ++
      currentStateHash.asBytesValue.any ++ timestamp.value.asBytesValue.any).bytes ++
      transactionBytes

  }

  override def compare(v: Value): Int =
    v match {
      case that: BlockValue =>
        val heightOrder = Ordering[BigInt].compare(this.height, that.height)
        if (heightOrder != 0) heightOrder
        else {
          val tsOrder = Ordering[Long].compare(this.timestamp.value, that.timestamp.value)
          if (tsOrder != 0) tsOrder
          else {
            val thisEncoding = rawBytes.asBytesValue.bcBase58
            val thatEncoding = that.rawBytes.asBytesValue.bcBase58
            Ordering[String].compare(thisEncoding, thatEncoding)
          }
        }
    }

}
