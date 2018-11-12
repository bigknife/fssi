package fssi.scp.types

case class SlotIndex(value: BigInt) extends AnyVal

object SlotIndex {
  trait Implicits {
    import fssi.base.implicits._
    implicit def slotIndexToBytes(slotIndex: SlotIndex): Array[Byte] =
      slotIndex.value.asBytesValue.bytes
  }
}
