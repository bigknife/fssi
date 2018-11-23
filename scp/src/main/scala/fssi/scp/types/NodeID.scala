package fssi.scp.types
import fssi.base.Base58

case class NodeID(value: Array[Byte]) {
  def ===(other: NodeID): Boolean = value sameElements other.value

  override def equals(obj: scala.Any): Boolean = obj match {
    case NodeID(bytes) => bytes sameElements value
    case _             => false
  }

  override def hashCode(): Int = BigInt(value).toInt

  override def toString: String = Base58.encode(value)
}

object NodeID {
  trait Implicits {

    implicit def nodeIdToBytes(nodeId: NodeID): Array[Byte] = nodeId.value
  }
}
