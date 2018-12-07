package fssi.interpreter.scp

import fssi.interpreter.UnsignedBytesSupport
import fssi.scp.types._
import fssi.types.biz._
import fssi.types.implicits._
import fssi.store.implicits._
import org.slf4j.LoggerFactory

case class BlockValue(block: Block) extends Value {
  lazy val log = LoggerFactory.getLogger(getClass)

  override def rawBytes: Array[Byte] = {
    val bytes = UnsignedBytesSupport.calculateUnsignedBlockBytes(block)
    bytes
  }

  override def hashCode: Int = {
    val heightCode    = block.height.hashCode()
    val curWorldState = block.curWorldState.asBytesValue.bcBase58.hashCode()
    val preWorldState = block.preWorldState.asBytesValue.bcBase58.hashCode()
    val r             = 31 * (31 * (31 * 17 + heightCode) + curWorldState) + preWorldState
    r
  }

  override def compare(v: Value): Int = {
    v match {
      case that: BlockValue =>
        val heightOrder = Ordering[BigInt].compare(this.block.height, that.block.height)
        if (heightOrder != 0) heightOrder
        else {
          val thisEncoding = rawBytes.asBytesValue.base64
          val thatEncoding = that.rawBytes.asBytesValue.base64
          val contentOrder = Ordering[String].compare(thisEncoding, thatEncoding)

          if (contentOrder != 0) {
            val timeOrder =
              Ordering[Long].compare(this.block.timestamp.value, that.block.timestamp.value)
            if (timeOrder != 0) timeOrder
            else contentOrder
          } else {
            contentOrder
          }
        }
      case _ => -1
    }
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
    import fssi.types.json.implicits._

    implicit val valueEncoder: Encoder[Value] = {
      case blockValue: BlockValue => blockValue.asJson
    }

    implicit val valueDecoder: Decoder[Value] = (hCursor: HCursor) => hCursor.as[BlockValue]
  }

}
