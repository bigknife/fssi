package fssi
package types
package base

/** hash for any data, a common hash, no more type info.
  */
case class Hash(value: Array[Byte])

object Hash {
  trait Implicits {
    implicit def hashToBytesValue(x: Hash): Array[Byte] = x.value
  }
}
