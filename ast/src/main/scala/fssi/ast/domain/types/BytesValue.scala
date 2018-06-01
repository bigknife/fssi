package fssi.ast.domain.types

import java.math.BigInteger

trait BytesValue {
  def bytes: Array[Byte]

  def hex: String = bytes.map("%2x" format _).mkString("")

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: BytesValue => x.bytes sameElements bytes
    case _ => false
  }
}

object BytesValue {
  case class SimpleBytesValue(bytes: Array[Byte]) extends BytesValue
  def apply(xs: Array[Byte]): BytesValue = SimpleBytesValue(xs)
  def apply(str: String): BytesValue     = SimpleBytesValue(str.getBytes("utf-8"))
  def decodeHex(hex: String): BytesValue = apply(new BigInteger(hex, 16).toByteArray)

  val Empty: BytesValue = SimpleBytesValue(Array.emptyByteArray)
}
