package fssi
package interpreter
package util

import types._
import com.typesafe.config._
import scala.collection.JavaConverters._

case class ConfigReader(conf: java.io.File) {
  lazy val config: Config = ConfigFactory.parseFile(conf)

  def readHost(): String = config.getString("core-node.host")
  def readPort(): Int    = config.getInt("core-node.port")
  def readSeeds(): Vector[Node.Addr] =
    config
      .getStringList("core-node.seeds")
      .asScala
      .toVector
      .map(Node.parseAddr(_))
      .filter(_.isDefined)
      .map(_.get)

}
