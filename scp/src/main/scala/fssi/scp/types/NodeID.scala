package fssi.scp.types

case class NodeID(value: Array[Byte]) extends AnyVal {
  def ===(other: NodeID): Boolean = value sameElements other.value
}

object NodeID {
  trait Implicits {

    implicit def nodeIdToBytes(nodeId: NodeID): Array[Byte] = nodeId.value
  }
}
