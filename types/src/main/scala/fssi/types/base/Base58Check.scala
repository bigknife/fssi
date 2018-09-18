package fssi
package types
package base

/** Base58Check encoding
  * @see https://github.com/bitcoinbook/bitcoinbook/blob/develop/ch04.asciidoc#base58check_encoding
  */
case class Base58Check(
  version: Byte,
  payload: Array[Byte]
    checksum: 
)

object Base58Check {
  sealed trait Checksum
  object Checksum {
    case object Empty extends Checksum
    case class DoubleSHA(bytes: (Byte, Byte, Byte, Byte)) {
      val array: Array[Byte] = Array(bytes._1, bytes._2, bytes._3, bytes._4)
    }
  }
}
