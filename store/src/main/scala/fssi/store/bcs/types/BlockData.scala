package fssi.store.bcs.types

case class BlockData(bytes: Array[Byte]) extends AnyVal {
  def ===(that: BlockData): Boolean = this.bytes sameElements that.bytes
}
