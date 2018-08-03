package fssi.types

import fssi.utils._

case class HexString(bytes: Array[Byte]) {
  override def toString(): String = {
    "0x" + noPrefix
  }

  def noPrefix: String = BytesUtil.toHex(bytes)
}
