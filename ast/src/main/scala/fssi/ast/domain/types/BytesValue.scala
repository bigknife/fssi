package fssi.ast.domain.types

trait BytesValue {
  def bytes: Array[Byte]
}

object BytesValue {
  case class SimpleBytesValue(bytes: Array[Byte]) extends BytesValue
  def apply(xs: Array[Byte]): BytesValue = SimpleBytesValue(xs)
  def apply(str: String): BytesValue     = SimpleBytesValue(str.getBytes("utf-8"))

  val Empty: BytesValue = SimpleBytesValue(Array.emptyByteArray)
}
