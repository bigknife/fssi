package fssi.interpreter.scp
import fssi.scp.types.{SlotIndex, Value}

case class FakeValue(slotIndex: SlotIndex) extends Value {
  override def rawBytes: Array[Byte] = Array.emptyByteArray
  override def compare(that: Value): Int = that match {
    case fakeValue: FakeValue =>
      Ordering[BigInt].compare(slotIndex.value, fakeValue.slotIndex.value)
    case _ => -1
  }

  override def equals(obj: scala.Any): Boolean = obj match {
    case fakeValue: FakeValue => fakeValue.rawBytes sameElements rawBytes
    case _                    => false
  }
}
