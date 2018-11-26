package fssi
package base

import scala.util._

/** Base type, the raw bytes
  *
  */
trait BytesValue[A] {

  /** the value's bytes presentation
    */
  def bytes: Array[Byte]

  def length: Int = bytes.length

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: BytesValue[_] => x.bytes sameElements bytes
    case _                => false
  }

  def ===(other: BytesValue[A]): Boolean = bytes sameElements other.bytes
}

object BytesValue {

  def empty[A]: BytesValue[A] = new BytesValue[A] {
    def bytes: Array[Byte] = Array.emptyByteArray
  }

  def apply[A](implicit I: BytesValue[A]): BytesValue[A] = I
  def summon[A](F: A => Array[Byte]): A => BytesValue[A] =
    (a: A) =>
      new BytesValue[A] {
        def bytes: Array[Byte] = F(a)
    }

  def decodeHex[A](hex: String): Option[BytesValue[A]] = {
    // seq size should be
    def loop(seq: Vector[Char], bytes: Vector[Byte]): Vector[Byte] = {
      seq.splitAt(2) match {
        case (Vector(), _) => bytes
        case (Vector(c1, c2), t) =>
          loop(t,
               bytes :+ ((Integer.parseInt(c1.toString, 16) << 4) | Integer.parseInt(c2.toString,
                                                                                     16)).toByte)
      }
    }
    Try {
      new BytesValue[A] {
        def bytes: Array[Byte] = loop(hex.toVector, Vector.empty).toArray
      }
    }.toOption
  }

  def unsafeDecodeHex[A](hex: String): BytesValue[A] = decodeHex(hex).get

  def decodeBase64[A](s: String): Option[BytesValue[A]] =
    Try {
      new BytesValue[A] { def bytes: Array[Byte] = java.util.Base64.getDecoder.decode(s) }
    }.toOption

  def unsafeDecodeBase64[A](s: String): BytesValue[A] = decodeBase64(s).get

  def decodeBcBase58[A](s: String): Option[BytesValue[A]] = Base58.decode(s).map { x =>
    new BytesValue[A] {
      def bytes: Array[Byte] = x
    }
  }

  def unsafeDecodeBcBase58[A](s: String): BytesValue[A] = decodeBcBase58(s).get

  final case class Ops[A](bv: BytesValue[A]) {
    def value: Array[Byte] = bv.bytes
    def hex: String        = bv.bytes.map("%02x" format _).mkString("")
    def utf8String: String = new String(bv.bytes, "utf-8")
    def base64: String     = java.util.Base64.getEncoder.encodeToString(bv.bytes)
    def append(other: BytesValue[A]): BytesValue[A] = new BytesValue[A] {
      def bytes: Array[Byte] = bv.bytes ++ other.bytes
    }
    def ++(other: BytesValue[A]): BytesValue[A] = append(other)
    def digest(implicit MD: java.security.MessageDigest): BytesValue[A] = new BytesValue[A] {
      def bytes: Array[Byte] = MD.digest(bv.bytes)
    }
    def bcBase58: String = Base58.encode(bv.bytes)

    def isEmpty: Boolean   = bv.bytes.length == 0
    def nonEmpty: Boolean  = bv.bytes.length != 0
    def isDefined: Boolean = bv.bytes.length != 0

    def any: BytesValue[Any] = new BytesValue[Any] {
      def bytes: Array[Byte] = bv.bytes
    }
  }

  final case class Syntax[A](a: A)(implicit F: A => Array[Byte]) {
    def asBytesValue: BytesValue[A] = summon(F)(a)
  }

  trait Implicits {
    implicit def toOps[A](bv: BytesValue[A]): Ops[A]             = Ops(bv)
    implicit def toSyntax[A](a: A)(implicit F: A => Array[Byte]) = Syntax(a)

    implicit def boolBytesValue(boolean: Boolean): Array[Byte] = (if (boolean) 1 else 0).toByte
    implicit def bigIntBytesValue(i: BigInt): Array[Byte]      = i.toByteArray
    implicit def intToBytesValue(i: Int): Array[Byte]          = bigIntBytesValue(BigInt(i))
    implicit def byteToBytesValue(b: Byte): Array[Byte]        = Array(b)
    implicit def longToBytesValue(l: Long): Array[Byte]        = bigIntBytesValue(BigInt(l))
    implicit def stringToBytesValue(s: String): Array[Byte]    = s.getBytes("utf-8")
    implicit def arrayToBytesValue[A](a: Array[A])(implicit F: A => Array[Byte]): Array[Byte] = {
      a.foldLeft(Array.emptyByteArray) { (acc, n) =>
        acc ++ F(n)
      }
    }
    implicit def arryByteToArrayByte(a: Array[Byte]): Array[Byte] = a

    implicit def optionToBytesValue[A](a: Option[A])(implicit F: A => Array[Byte]): Array[Byte] = {
      if (a.isEmpty) Array.emptyByteArray
      else F(a.get)
    }
  }

  object implicits extends Implicits

}
