package fssi
package trie

import java.nio.ByteBuffer

import utils._

/** byte array trait
  * defined that instances of Bytes should compute it's determined bytes
  */
trait Bytes[A] {
  def determinedBytes(a: A): Array[Byte]
  def hash(a: A): Array[Byte] = crypto.hash(determinedBytes(a))

  def to(bytes: Array[Byte]): A
}

object Bytes {
  def summon[A](f: A => Array[Byte])(v: Array[Byte] => A): Bytes[A] = new Bytes[A] {
    override def determinedBytes(a: A): Array[Byte] = f(a)
    override def to(bytes: Array[Byte]): A          = v(bytes)
  }

  trait Implicits {
    implicit val stringToBytes: Bytes[String] =
      summon[String](_.getBytes("utf-8"))(new String(_, "utf-8"))
    implicit val bytesToBytes: Bytes[Array[Byte]] = summon[Array[Byte]](x => x)(x => x)
    implicit val byteToBytes: Bytes[Byte]         = summon[Byte](x => Array(x))(x => x(0))
    implicit val charToBytes: Bytes[Char]         = summon[Char](x => Array(x.toByte))(x => x(0).toChar)
    implicit val intToBytes: Bytes[Int] = summon[Int]({ x =>
      val b = ByteBuffer.allocate(4)
      b.putInt(x)
      b.array()
    })({ x =>
      val b = ByteBuffer.allocate(4)
      b.put(x)
      b.getInt
    })
    implicit val longToBytes: Bytes[Long] = summon[Long]({ x =>
      val b = ByteBuffer.allocate(8)
      b.putLong(x)
      b.array()
    })({ x =>
      val b = ByteBuffer.allocate(8)
      b.put(x)
      b.getLong
    })
  }

  trait Syntax {
    implicit final class Ops[A](a: A)(implicit E: Bytes[A]) {
      def hash: Array[Byte]            = E.hash(a)
      def determinedBytes: Array[Byte] = E.determinedBytes(a)
    }
  }

  object implicits extends Implicits with Syntax
}
