package fssi.types

case class BytesValue(value: Array[Byte]) {
  def toBase64String: Base64String = Base64String(value)
  def toHexString: HexString = HexString(value)
}

object BytesValue {

  trait Syntax {
    implicit def arrayBytesToBytesValue(value: Array[Byte]): BytesValue = BytesValue(value)
  }
}
