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
    def decode(bytes: Array[Byte]): Option[Slot] = {
      val bb = ByteBuffer.wrap(bytes)

      def _loop(bb: ByteBuffer, acc: Vector[Cell]): Vector[Cell] = {
        if(bb.position() == bb.limit()) acc
        else {
          val flag = Array(0.toByte)
          bb.get(flag)
          if(flag(0) != 0x30) throw new RuntimeException("Slot Cell should start with 0x30")
          else {
            val idx = bb.getInt
            val radix = Radix.Hex.ofIndex(idx)
            val keyLength = bb.getInt
            val keyBytes = Array.fill(keyLength)(0.toByte)
            val key = Key.wrap(keyBytes)
            acc :+ Cell(radix, key)
          }
        }
      }

      scala.util.Try {
        SimpleSlot(_loop(bb, Vector.empty))
      }.toOption
    }
  }


}
