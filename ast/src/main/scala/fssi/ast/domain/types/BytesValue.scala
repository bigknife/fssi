package fssi.ast.domain.types

trait BytesValue {
  def bytes: Array[Byte]

  def hex: String        = bytes.map("%02x" format _).mkString("")
  def utf8String: String = new String(bytes, "utf-8")
  def base64: String     = java.util.Base64.getEncoder.encodeToString(bytes)

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: BytesValue => x.bytes sameElements bytes
    case _             => false
  }

  override def toString: String = s"Hex($hex)"
}

object BytesValue {
  case class SimpleBytesValue(bytes: Array[Byte]) extends BytesValue
  def apply(xs: Array[Byte]): BytesValue = SimpleBytesValue(xs)
  def apply(str: String): BytesValue     = SimpleBytesValue(str.getBytes("utf-8"))
  def decodeHex(hex: String): BytesValue =
    apply(javax.xml.bind.DatatypeConverter.parseHexBinary(hex))
  def decodeBase64(base64: String): BytesValue = apply(java.util.Base64.getDecoder.decode(base64))

  val Empty: BytesValue = SimpleBytesValue(Array.emptyByteArray)
}
