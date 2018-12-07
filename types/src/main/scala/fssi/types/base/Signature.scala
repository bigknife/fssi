package fssi.types
package base

/** Signature
  */
case class Signature(value: Array[Byte]) extends AnyVal {
  def ===(other: Signature): Boolean = value sameElements other.value
}

object Signature {
  def empty: Signature = Signature(Array.emptyByteArray)

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
    implicit def signatureToBytesValue(s: Signature): Array[Byte]  = s.value
  }
}
