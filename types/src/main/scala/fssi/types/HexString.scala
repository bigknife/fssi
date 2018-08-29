package fssi
package types

import utils._

case class HexString(bytes: Array[Byte]) {
  override def toString: String = {
    "0x" + noPrefix
  }

  def noPrefix: String = BytesUtil.toHex(bytes)

  def toBytesValue: BytesValue = BytesValue(bytes)

  def isEmpty: Boolean = bytes.isEmpty

  override def equals(that: Any): Boolean = that match {
    case HexString(thatBytes) => bytes sameElements thatBytes
    case _ => false
  }
}

object HexString {
  def decode(hexString: String): HexString = {
    hexString.take(2) match {
      case "0x" => HexString(BytesUtil.decodeHex(hexString.drop(2)))
      case _ => HexString(BytesUtil.decodeHex(hexString))
    }
  }

  def empty: HexString = HexString(Array.emptyByteArray)
}
