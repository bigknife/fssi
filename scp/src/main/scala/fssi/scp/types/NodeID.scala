package fssi.scp.types
import fssi.base.Base58

case class NodeID(value: Array[Byte]) extends AnyVal {
  def ===(other: NodeID): Boolean = value sameElements other.value

  override def toString: String = Base58.encode(value)
}

object NodeID {
  trait Implicits {

    implicit def nodeIdToBytes(nodeId: NodeID): Array[Byte] = nodeId.value
  }
}
