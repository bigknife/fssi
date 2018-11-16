package fssi.store.bcs.types

case class MetaData(bytes: Array[Byte]) extends AnyVal {
  def ===(that: MetaData): Boolean = this.bytes sameElements that.bytes
}
