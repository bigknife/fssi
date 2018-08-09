package fssi.utils
package trie

sealed trait Nibble {
  def value: Byte

  def toChar: Char              = "%x".format(value).head
  override def toString: String = s"$toChar"

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: Nibble => value == x.value
    case _         => false
  }
}

object Nibble {
  case class SimpleNibble(byte: Byte) extends Nibble {
    val value: Byte = (byte & 0x0F).toByte
  }

  /** create a nibble from an int(0 .. 15)
    */
  def apply(i: Int): Nibble = {
    require(i < 16 && i >= 0, "Nibble should in [0..16)")
    SimpleNibble(i.toByte)
  }
  def apply(i: Byte): Nibble = {
    require(i < 16 && i >= 0, "Nibble should in [0..16)")
    SimpleNibble(i)
  }

  sealed trait Sequence {
    def nibbles: Vector[Nibble]

    // add nibble prefix before nibbles to describe that node is a leaf
    // see: https://github.com/ethereum/wiki/wiki/Patricia-Tree
    def prefixedWithLeaf: Sequence
    def unprefixedWithLeaf: Sequence
    def leafPreifx: Sequence

    // add nibble prefix before nibbles to describe that node is an extension
    def prefixedWithExtension: Sequence
    def extensionPrefix: Sequence
    def unprefixedWithExtension: Sequence

    def :+(nibble: Nibble): Sequence
    def drop(n: Int): Sequence
    def take(n: Int): Sequence
    def head: Nibble     = nibbles.head
    def isEmpty: Boolean = nibbles.isEmpty
    def size: Int        = nibbles.size

    override def toString: String = nibbles.map(_.toString).mkString("")

    override def equals(obj: scala.Any): Boolean = obj match {
      case x: Sequence => x.nibbles.equals(nibbles)
      case _           => false
    }

    def toByteArray(): Array[Byte] = {
      require(nibbles.size % 2 == 0, "nibbles'to byte array requrie odd")
      nibbles
        .foldLeft((0, Array.emptyByteArray)) {
          case ((i, acc), n) if i % 2 == 0 =>
            (i + 1, acc :+ (n.value << 4).toByte)
          case ((i, acc), n) =>
            val idx = i / 2
            val b   = acc(idx)
            acc.update(idx, (b | n.value).toByte)
            (i + 1, acc)
        }
        ._2
    }
    /*
      nibbles
        .dropRight(1)
        .zip(nibbles.drop(1))
        .map {
          case (x1, x2) => ((x1.value << 4).toByte | x2.value).toByte
        }
        .toArray
   */

  }

  case class SimpleSequence(nibbles: Vector[Nibble]) extends Sequence {

    override def :+(nibble: Nibble): Sequence = SimpleSequence(nibbles :+ nibble)

    override def drop(n: Int): Sequence = SimpleSequence(nibbles.drop(n))

    override def take(n: Int): Sequence = SimpleSequence(nibbles.take(n))

    override def prefixedWithLeaf: Sequence = {
      val nibble =
        if ((nibbles.length % 2) == 0) Vector(Nibble(2), Nibble(0)) else Vector(Nibble(3))
      SimpleSequence(nibble ++ nibbles)
    }

    override def unprefixedWithLeaf: Sequence = {
      if ((nibbles.length % 2 == 0) && nibbles.take(2) == Vector(Nibble(2), Nibble(0))) {
        drop(2)
      } else if (nibbles.take(1) == Vector(Nibble(3))) {
        drop(1)
      } else throw new IllegalArgumentException("Not a legal prefixed leaf node")
    }

    override def leafPreifx: Sequence = {
      if ((nibbles.length % 2 == 0) && nibbles.take(2) == Vector(Nibble(2), Nibble(0))) {
        take(2)
      } else if (nibbles.take(1) == Vector(Nibble(3))) {
        take(1)
      } else throw new IllegalArgumentException("Not a legal prefixed leaf node")
    }

    override def prefixedWithExtension: Sequence = {
      val nibble =
        if ((nibbles.length % 2) == 0) Vector(Nibble(0), Nibble(0)) else Vector(Nibble(1))
      SimpleSequence(nibble ++ nibbles)
    }

    override def extensionPrefix: Sequence = {
      if (nibbles.length % 2 == 0 && nibbles.take(2) == Vector(Nibble(0), Nibble(0))) {
        take(2)
      } else if (nibbles.take(1) == Vector(Nibble(1))) {
        take(1)
      } else throw new IllegalArgumentException("Not a legal prefixed extension node")
    }

    override def unprefixedWithExtension: Sequence = {
      if (nibbles.length % 2 == 0 && nibbles.take(2) == Vector(Nibble(0), Nibble(0))) {
        drop(2)
      } else if (nibbles.take(1) == Vector(Nibble(1))) {
        drop(1)
      } else throw new IllegalArgumentException("Not a legal prefixed extension node")
    }
  }

  object Sequence {
    def apply(bytes: Array[Byte]): Sequence =
      SimpleSequence(
        bytes.toVector
          .map(SimpleNibble.apply)
      )
    // a byte divided to two, upper and lower
    def divide(bytes: Array[Byte]): Sequence = {
      SimpleSequence(
        bytes
          .foldLeft(Array.emptyByteArray) {
            case (acc, n) =>
              acc ++ Array(((n >> 4) & 0x0F).toByte, (n & 0x0F).toByte)
          }
          .toVector
          .map(SimpleNibble.apply))
    }
    def empty: Sequence = SimpleSequence(Vector.empty)
  }
}
