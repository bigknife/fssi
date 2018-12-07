package fssi.types.biz

import fssi.types.implicits._

case class Node(
    address: Node.Addr,
    account: Account
) {

  private lazy val show: String =
    s"$address(${account.id.asBytesValue.bcBase58}:${account.pubKey.asBytesValue.bcBase58})"

  override def toString: String = show
}

object Node {
  case class ConsensusNode(value: Node)   extends AnyVal
  case class ApplicationNode(value: Node) extends AnyVal
  case class ServiceNode(value: Node)     extends AnyVal

  case class Addr(host: String, port: Int) {
    override def toString(): String = s"$host:$port"
  }
  def parseAddr(addr: String): Option[Addr] = addr.split(":") match {
    case Array(ip, port) => scala.util.Try { Addr(ip, port.toInt) }.toOption
    case _               => None
  }
}
