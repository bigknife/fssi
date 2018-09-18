package fssi
package types
package base

/** Base58Check encoding
  * @see https://github.com/bitcoinbook/bitcoinbook/blob/develop/ch04.asciidoc#base58check_encoding
  */
case class Base58Check(
    version: Byte,
    payload: Array[Byte],
    checksum: Base58Checksum = Base58Check.emptyChecksum
) {
  import Base58Check._

  lazy val uncheckedBytes: Array[Byte] = version +: payload

  def isSane: Boolean = {
    val c1 = doubleSHA256(uncheckedBytes)
    checksum == c1
  }

  def resetChecksum: Base58Check = copy(checksum = doubleSHA256(uncheckedBytes))

}

object Base58Check {
  sealed trait Checksum
  object Checksum {
    case object Empty extends Checksum
    case class DoubleSHA256(bytes: (Byte, Byte, Byte, Byte)) extends Checksum {
      val array: Array[Byte] = Array(bytes._1, bytes._2, bytes._3, bytes._4)
    }
  }
  def emptyChecksum: Checksum = Checksum.Empty
  def doubleSHA256(source: Array[Byte]): Checksum = if (source.isEmpty) emptyChecksum else {
    val sha256 = java.security.MessageDigest.getInstance("SHA-256")
    import sha256._
    val b4 = digest(digest(source)).take(4)
    Checksum.DoubleSHA256((b4(0), b4(1), b4(2), b4(3)))
  }
  def doubleSHA256[A](source: BytesValue[A]): Checksum = doubleSHA256(source.bytes)

  trait Implicits {
    implicit def base58checkToBytesValue(x: Base58Check): Array[Byte] = x match {
      case x0@Base58Check(_, _, Checksum.Empty) =>  x0.uncheckedBytes
      case x0@Base58Check(_, _, x1@Checksum.DoubleSHA256(_)) => x0.uncheckedBytes ++ x1.array
    }
  }
}
