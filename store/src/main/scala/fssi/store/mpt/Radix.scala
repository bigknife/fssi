package fssi.store.mpt

sealed trait Radix {
  def index: Int
  def representation: String

  override def toString: String = s"$index-$representation"
}
object Radix {
  object Hex {
    private val bases: Array[Char] = Array(
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )
    def ofIndex(i: Int): Radix = new Radix {
      override def index: Int             = i % bases.length
      override def representation: String = bases(index).toString
    }
    def ofRepr(b: Byte): Option[Radix] = ofRepr(b.toChar)
    def ofRepr(i: Int): Option[Radix]  = ofRepr(i.toChar)
    def ofRepr(c: Char): Option[Radix] =
      if (c >= bases.head && c <= bases(bases.last)) {
        Some(new Radix {
          override def index: Int = bases.indexOf(c)

          override def representation: String = c.toString
        })
      } else None

  }
}
