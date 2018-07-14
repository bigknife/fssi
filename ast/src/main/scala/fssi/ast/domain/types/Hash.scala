package fssi.ast.domain.types

case class Hash(bytes: Array[Byte]) extends BytesValue

object Hash {
  def fromHexString(hex: String): Hash = Hash(BytesValue.decodeHex(hex).bytes)
  def empty: Hash = Hash(Array.emptyByteArray)
}

