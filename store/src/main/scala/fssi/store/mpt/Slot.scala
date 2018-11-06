package fssi.store.mpt

import java.nio.ByteBuffer

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
    def encode(slot: Slot): Array[Byte] = slot match {
      case SimpleSlot(cells) =>
        cells.foldLeft(Array.emptyByteArray) {(acc, n) =>
          val bytes = {
            // 0x30 + 4(int(idx)) + 4(int(key size)) + key bytes
            val bb = ByteBuffer.allocate(1 + 4 + 4 + n.key.length)
            bb.put(0x30.toByte)
            bb.putInt(n.radix.index)
            bb.putInt(n.key.length)
            bb.put(n.key.bytes)
            bb.array()
          }
          acc ++ bytes.array
        }
    }
  }


}
