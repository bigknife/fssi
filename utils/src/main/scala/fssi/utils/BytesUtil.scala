package fssi.utils

sealed trait BytesUtil {
  def toHex(bytes: Array[Byte], upper: Boolean = false): String = {
    val fmt = if (upper) "%02X" else "%02x"
    bytes.map(fmt format _).mkString("")
  }

  def toBase64(bytes: Array[Byte]): String =
    java.util.Base64.getEncoder.encodeToString(bytes)
}

object BytesUtil extends BytesUtil
