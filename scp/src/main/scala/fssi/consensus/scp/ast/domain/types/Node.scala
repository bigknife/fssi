package fssi.consensus.scp.ast.domain.types

case class Node()

object Node {
  case class ID(bytes: Array[Byte]) {
    override def equals(obj: scala.Any): Boolean = obj match {
      case ID(x) => x sameElements bytes
      case _ => false
    }
  }


}
