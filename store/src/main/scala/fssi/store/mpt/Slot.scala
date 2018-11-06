package fssi.store.mpt

sealed trait Slot {
  def update(idx: Int, key: Key): Slot
  def update(idx: Byte, key: Key): Slot = update(idx.toInt, key)
  def get(idx: Int): Slot.Cell
}

object Slot {
  private case class SimpleSlot(cells: Vector[Cell]) extends Slot {
    override def update(idx: Int, key: Key): Slot = {
      copy(cells = cells.foldLeft(Vector.empty[Cell]) { (acc, n) =>
        if (n.radix.index == idx) acc :+ Cell(Radix.Hex.ofRepr(idx).get, key)
        else acc :+ n
      })
    }

    override def get(idx: Int): Cell = {
      cells.find(_.radix.index == idx).get
    }
  }
  case class Cell(radix: Radix, key: Key)

  object Hex {
    def empty: Slot = SimpleSlot(Vector.empty)
  }
}
