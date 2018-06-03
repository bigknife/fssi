package fssi.ast.domain.types

import scala.util.Try

/** type to identify p2p node address */
case class Seed(
    port: Int,
    ip: String
) {
  override def toString: String = s"$ip:$port"
}

object Seed {
  def fromString(s: String): Option[Seed] = s.split(":") match {
    case Array(x, y) => Try { Seed(y.toInt, x) }.toOption
    case _ => None
  }

  def from(s: String): Seed = fromString(s).get
}
