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
}

object Bytes {
  def summon[A](f: A => Array[Byte]): Bytes[A] = (a: A) => f(a)

  trait Implicits {
    implicit val stringToBytes: Bytes[String]     = summon[String](_.getBytes("utf-8"))
    implicit val bytesToBytes: Bytes[Array[Byte]] = summon[Array[Byte]](x => x)
    implicit val byteToBytes: Bytes[Byte] = summon[Byte](x => Array(x))
    implicit val charToBytes: Bytes[Char]         = summon[Char](x => Array(x.toByte))
    implicit val intToBytes: Bytes[Int] = summon[Int] { x =>
      val b = ByteBuffer.allocate(4)
      b.putInt(x)
      b.array()
    }
    implicit val longToBytes: Bytes[Long] = summon[Long] { x =>
      val b = ByteBuffer.allocate(8)
      b.putLong(x)
      b.array()
    }
  }

  trait Syntax {
    implicit final class Ops[A](a: A)(implicit E: Bytes[A]) {
      def hash: Array[Byte]            = E.hash(a)
      def determinedBytes: Array[Byte] = E.determinedBytes(a)
    }
  }

  object implicits extends Implicits with Syntax
}
