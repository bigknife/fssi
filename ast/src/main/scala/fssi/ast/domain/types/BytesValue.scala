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
  def decodeHex(hex: String): BytesValue = {

    // seq size should be
    def loop(seq: Vector[Char], bytes: Vector[Byte]): Vector[Byte] = {
      seq.splitAt(2) match {
        case (Vector(), _) => bytes
        case (Vector(c1, c2), t) =>
          loop(t, bytes :+ ((Integer.parseInt(c1.toString, 16) << 4) | Integer.parseInt(c2.toString, 16)).toByte)
      }
    }

    apply(loop(hex.toVector, Vector.empty).toArray)

    // jdk9+ can't work
    //apply(javax.xml.bind.DatatypeConverter.parseHexBinary(hex))
  }

  def decodeBase64(base64: String): BytesValue = apply(java.util.Base64.getDecoder.decode(base64))

  val Empty: BytesValue = SimpleBytesValue(Array.emptyByteArray)
}
