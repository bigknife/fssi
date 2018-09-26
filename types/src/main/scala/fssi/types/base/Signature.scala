package fssi.types
package base

/** Signature
  */
case class Signature(value: Array[Byte]) extends AnyVal

object Signature {
  def empty: Signature = Signature(Array.emptyByteArray)
  
  trait Implicits {
    implicit def signatureToBytesValue(s: Signature): Array[Byte]  = s.value
  }
}
