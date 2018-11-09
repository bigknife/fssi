package fssi.store.bcs.types

case class StateData(bytes: Array[Byte]) extends AnyVal {
  def ===(that: StateData): Boolean = bytes sameElements that.bytes
}
