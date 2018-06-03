package fssi.ast.domain

import java.math.BigInteger

import fssi.ast.domain.types.Account

case class Node(
    port: Int,
    ip: String,
    nodeType: Node.Type,
    boundAccount: Option[Account],
    seeds: Vector[String],
    runtimeId: Option[Node.ID] = None
) {
  def id: Node.ID = Node.ID.from(port, ip, nodeType)
}

object Node {
  case class ID(value: String)

  object ID {
    def from(port: Int, ip: String, nodeType: Node.Type): ID = {
      val s = s"$ip:$port:${nodeType.toString}"
      val i = new BigInteger(s.getBytes)
      ID(s"$i")
    }
  }

  sealed trait Type
  object Type {
    case object Nymph   extends Type
    case object Warrior extends Type
    case object Mirror  extends Type

    def apply(s: String): Type = s match {
      case x if x equalsIgnoreCase "nymph"   => Nymph
      case x if x equalsIgnoreCase "warrior" => Warrior
      case _                                 => Mirror
    }
  }

}
