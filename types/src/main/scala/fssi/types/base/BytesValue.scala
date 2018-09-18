package fssi
package types
package base

import utils._

/** Base type, the raw bytes
  *
  */
trait BytesValue[A] {

  /** the value's bytes presentation
	  */
  def bytes: Array[Byte]

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: BytesValue[_] => x.bytes sameElements bytes
    case _                => false
  }
}

object BytesValue {

  def apply[A](implicit I: BytesValue[A]): BytesValue[A] = I

  final class Ops[A](bv: BytesValue[A]) {
    def hex: String        = bv.bytes.map("%02x" format _).mkString("")
    def utf8String: String = new String(bv.bytes, "utf-8")
    def base64: String     = java.util.Base64.getEncoder.encodeToString(bv.bytes)
  }

  final class Syntax[A](a: A)(implicit F: A => BytesValue[A]) {
    def asBytesValue: BytesValue[A] = F(a)
  }

  trait Implicits {
    implicit def toOps[A](bv: BytesValue[A]): Ops[A] = new Ops(bv)
  }

}
