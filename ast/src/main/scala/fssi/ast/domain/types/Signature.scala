package fssi.ast.domain.types

/** signature data */
case class Signature(bytes: Array[Byte]) extends BytesValue

object Signature {
  def apply(str: String): Signature = Signature(str.getBytes("utf-8"))
  def apply(bytesValue: BytesValue): Signature = Signature(bytesValue.bytes)
  val Empty: Signature = Signature(Array.emptyByteArray)
}
