package fssi.scp.types

case class Signature(value: Array[Byte]) extends AnyVal

object Signature {
  trait Implicits {
    implicit def signatureToBytes(signature: Signature): Array[Byte] = signature.value
  }
}
