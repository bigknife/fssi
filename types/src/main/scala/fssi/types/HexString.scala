package fssi.types

import fssi.utils._

case class HexString(bytes: Array[Byte]) {
  override def toString(): String = {
    "0x" + noPrefix
  }

  def noPrefix: String = BytesUtil.toHex(bytes)
}

object HexString {
  def decode(hexString: String): HexString = {
    def loop(seq: Vector[Char], bytes: Vector[Byte]): Vector[Byte] = {
      seq.splitAt(2) match {
        case (Vector(), _) => bytes
        case (Vector(c1, c2), t) =>
          loop(t, bytes :+ ((Integer.parseInt(c1.toString, 16) << 4) | Integer.parseInt(c2.toString, 16)).toByte)
      }
    }

    HexString(loop(hexString.toVector, Vector.empty).toArray)
  }
}
