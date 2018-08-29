package fssi.utils

sealed trait BytesUtil {
  def toHex(bytes: Array[Byte], upper: Boolean = false): String = {
    val fmt = if (upper) "%02X" else "%02x"
    bytes.map(fmt format _).mkString("")
  }

  def toBase64(bytes: Array[Byte]): String =
    java.util.Base64.getEncoder.encodeToString(bytes)

  def decodeHex(hex: String): Array[Byte] = {
    val withouPrefix = if (hex.startsWith("0x")) hex.drop(2) else hex
    // seq size should be
    def loop(seq: Vector[Char], bytes: Vector[Byte]): Vector[Byte] = {
      seq.splitAt(2) match {
        case (Vector(), _) => bytes
        case (Vector(c1, c2), t) =>
          loop(t,
               bytes :+ ((Integer.parseInt(c1.toString, 16) << 4) | Integer.parseInt(c2.toString,
                                                                                     16)).toByte)
      }
    }

    loop(withouPrefix.toVector, Vector.empty).toArray
  }

  def decodeBase64(base64: String): Array[Byte] = java.util.Base64.getDecoder.decode(base64)
}

object BytesUtil extends BytesUtil
