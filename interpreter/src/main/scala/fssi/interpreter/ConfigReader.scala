package fssi
package interpreter

import types._
import com.typesafe.config._
import scala.collection.JavaConverters._

case class ConfigReader(conf: java.io.File) {
  lazy val config: Config = ConfigFactory.parseFile(conf)

  def readHost(): String = config.getString("p2p.host")
  def readPort(): Int    = config.getInt("p2p.port")
  def readSeeds(): Vector[Node.Addr] =
    config
      .getStringList("p2p.seeds")
      .asScala
      .toVector
      .map(Node.parseAddr(_))
      .filter(_.isDefined)
      .map(_.get)
  def readCoreNodeAccount(): Account = Account(
    publicKey = HexString.decode(config.getString("p2p.account.publicKey")),
    encryptedPrivateKey =
      HexString.decode(config.getString("p2p.account.encryptedPrivateKey")),
    iv = HexString.decode(config.getString("p2p.account.iv"))
  )

  def readJsonRpcPort(): Int    = config.getInt("edge-node.jsonrpc.port")
  def readJsonRpcHost(): String = config.getString("edge-node.jsonrpc.host")

}
