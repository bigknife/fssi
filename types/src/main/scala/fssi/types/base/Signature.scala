package fssi.types
package base

/** Signature
  */
case class Signature(value: Array[Byte]) extends AnyVal {
  def ===(other: Signature): Boolean = value sameElements other.value
}

object Signature {
  def empty: Signature = Signature(Array.emptyByteArray)
  
  trait Implicits {
    implicit def signatureToBytesValue(s: Signature): Array[Byte]  = s.value
  }
}
