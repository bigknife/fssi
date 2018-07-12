package fssi.ast.domain

import java.math.BigInteger

import fssi.ast.domain.types.{Account, BytesValue}

case class Node(
    address: Node.Address,
    nodeType: Node.Type,
    accountPublicKey: BytesValue,
    accountPrivateKey: BytesValue,
    seeds: Vector[String],
    runtimeId: Option[Node.ID] = None
) {
  def id: Node.ID = Node.ID.from(address.port, address.ip, nodeType)

  override def toString: String = runtimeId match {
    case Some(x) => s"Node(${id.value}/${x.value})"
    case None    => s"Node(${id.value})"
  }
}

object Node {
  case class ID(value: String)

  case class Address(ip: String, port: Int)
  object Address {
    def apply(s: String): Address = s.split(":") match {
      case Array(ip, port) if "^\\d{2,5}$".r.pattern.matcher(port).matches() =>
        Address(ip, port.toInt)
      case _ => Address(s, 0)
    }
  }

  def address(ip: String, port: Int): Address = Address(ip, port)
  def address(s: String): Address             = Address(s)

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
