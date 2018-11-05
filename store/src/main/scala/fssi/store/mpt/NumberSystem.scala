package fssi.store.mpt

import fssi.store.mpt.NumberSystem.Radix

trait NumberSystem {
  def radixes: Array[Radix]
  def radixOf(index: Int): Radix = radixes(index % radixes.length)
}

object NumberSystem {

  sealed trait Radix {
    def value: Int
    def representation: String

    override def toString: String = representation
  }

  object Radix {
    def apply(v: Int, repr: String): Radix = new Radix {
      override val value: Int = v

      override val representation: String = repr
    }
  }

  object Hex extends NumberSystem {
    override def radixes: Array[Radix] = Array(
      Radix(0, "0"),
      Radix(1, "1"),
      Radix(2, "2"),
      Radix(3, "3"),
      Radix(4, "4"),
      Radix(5, "5"),
      Radix(6, "6"),
      Radix(7, "7"),
      Radix(8, "8"),
      Radix(9, "9"),
      Radix(10, "a"),
      Radix(11, "b"),
      Radix(12, "c"),
      Radix(13, "d"),
      Radix(14, "e"),
      Radix(15, "f")
    )
  }

}
