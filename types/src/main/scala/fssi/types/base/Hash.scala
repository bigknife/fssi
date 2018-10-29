package fssi
package types
package base

/** hash for any data, a common hash, no more type info.
  */
case class Hash(value: Array[Byte]) extends AnyVal {
  def ===(other: Hash): Boolean = value sameElements other.value
}

object Hash {

  def empty: Hash = Hash(Array.emptyByteArray)

  trait Implicits {
    implicit def hashToBytesValue(x: Hash): Array[Byte] = x.value
  }
}
