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

  sealed trait VerifyResult {
    def apply(): Boolean
  }
  case object Passed extends VerifyResult {
    def apply(): Boolean = true
  }
  case object Tampered extends VerifyResult {
    def apply(): Boolean = false
  }

  trait Implicits {
    implicit def hashToBytesValue(x: Hash): Array[Byte] = x.value
  }
}
