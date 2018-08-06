package fssi.types

case class Node(
  address: Node.Addr,
  account: Option[Account]
)

object Node {
  case class Addr(host: String, port: Int) {
    override def toString(): String = s"$host:$port"
  }

  def parseAddr(addr: String): Option[Addr] = addr.split(":") match {
    case Array(ip, port) => scala.util.Try {Addr(ip, port.toInt)}.toOption
    case _ => None
  }
}
