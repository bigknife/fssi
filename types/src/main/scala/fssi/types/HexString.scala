package fssi
package types

import utils._

case class HexString(bytes: Array[Byte]) {
  override def toString(): String = {
    "0x" + noPrefix
  }

  def noPrefix: String = BytesUtil.toHex(bytes)

  def toBytesValue: BytesValue = BytesValue(bytes)
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
