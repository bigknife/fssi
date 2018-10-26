package fssi.scp.types

case class NodeID(value: Array[Byte]) extends AnyVal {
  def ===(other: NodeID): Boolean = value sameElements other.value
}
