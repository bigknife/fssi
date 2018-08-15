package fssi
package types

import utils._

/**
  * Hash is a hash value of a data block
  * represented by a hexstring
  */
case class Hash(value: HexString) {
  override def toString(): String = value.toString
  def bytes: Array[Byte] = value.bytes

  def toBytesValue: BytesValue = value.toBytesValue
}

object Hash {
  def empty: Hash = Hash(HexString.empty)
  def apply(bytes: Array[Byte]): Hash = Hash(HexString(bytes))
}
