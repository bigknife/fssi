package fssi.types.biz

case class Node(
  address: Node.Addr,
  account: Account
)

object Node {
  case class ConsensusNode(value: Node) extends AnyVal
  case class ApplicationNode(value: Node) extends AnyVal
  case class ServiceNode(value: Node) extends AnyVal

  case class Addr(host: String, port: Int) {
    override def toString(): String = s"$host:$port"
  }
  def parseAddr(addr: String): Option[Addr] = addr.split(":") match {
    case Array(ip, port) => scala.util.Try {Addr(ip, port.toInt)}.toOption
    case _ => None
  }
}
