package fssi.interpreter.scp

import fssi.interpreter.UnsignedBytesSupport
import fssi.scp.types._
import fssi.store.mpt.Hash
import fssi.types.biz._
import fssi.types.implicits._
import fssi.store.implicits._

case class BlockValue(block: Block) extends Value {

  override def rawBytes: Array[Byte] = {
    val bytes = UnsignedBytesSupport.calculateUnsignedBlockBytes(block)
    bytes
  }

  override def hashCode: Int = {
    31 * (31 * (31 * 17 + block.height.hashCode()) + block.curWorldState
      .hashCode()) + block.preWorldState.hashCode()
  }

  override def compare(v: Value): Int =
    v match {
      case that: BlockValue =>
        val heightOrder = Ordering[BigInt].compare(this.block.height, that.block.height)
        if (heightOrder != 0) heightOrder
        else {
          val tsOrder =
            Ordering[Long].compare(this.block.timestamp.value, that.block.timestamp.value)
          if (tsOrder != 0) tsOrder
          else {
            val thisEncoding = rawBytes.asBytesValue.bcBase58
            val thatEncoding = that.rawBytes.asBytesValue.bcBase58
            Ordering[String].compare(thisEncoding, thatEncoding)
          }
        }
      case _ => -1
    }

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case value: BlockValue => value.rawBytes sameElements rawBytes
      case _                 => false
    }
  }
}

object BlockValue {

  object implicits {
    import io.circe._
    import io.circe.generic.auto._
    import io.circe.syntax._

    implicit val valueEncoder: Encoder[Value] = {
      case blockValue: BlockValue => blockValue.asJson
    }

    implicit val valueDecoder: Decoder[Value] = (hCursor: HCursor) => hCursor.as[BlockValue]
  }

}
