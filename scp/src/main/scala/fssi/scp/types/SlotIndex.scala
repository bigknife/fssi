package fssi.scp.types

case class SlotIndex(value: BigInt) extends AnyVal {
  def +(i: BigInt): SlotIndex = SlotIndex(value + i)
  def -(i: BigInt): SlotIndex = SlotIndex(value - i)
}

object SlotIndex {
  trait Implicits {
    import fssi.base.implicits._
    implicit def slotIndexToBytes(slotIndex: SlotIndex): Array[Byte] =
      slotIndex.value.asBytesValue.bytes
  }
}
