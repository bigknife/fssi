package fssi.types

/**
  * Hash is a hash value of a data block
  * represented by a hexstring
  */
case class Hash(value: HexString) {
  override def toString(): String = value.toString
  def bytes: Array[Byte] = value.bytes
}

object Hash {
  def empty: Hash = Hash(HexString.empty)
  def apply(bytes: Array[Byte]): Hash = Hash(HexString(bytes))
}
