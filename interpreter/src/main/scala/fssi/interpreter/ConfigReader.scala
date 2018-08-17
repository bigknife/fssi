package fssi
package interpreter

import bigknife.scalap.ast.types._
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
      .map(Node.parseAddr)
      .filter(_.isDefined)
      .map(_.get)
  def readCoreNodeAccount(): Account = Account(
    publicKey = HexString.decode(config.getString("p2p.account.publicKey")),
    encryptedPrivateKey = HexString.decode(config.getString("p2p.account.encryptedPrivateKey")),
    iv = HexString.decode(config.getString("p2p.account.iv"))
  )

  def readJsonRpcPort(): Int    = config.getInt("edge-node.jsonrpc.port")
  def readJsonRpcHost(): String = config.getString("edge-node.jsonrpc.host")

  def readQuorumSet(): QuorumSet = {

    def configToSimpleQuorumSet(conf: Config): QuorumSet =
      QuorumSet.simple(conf.getInt("threshold"),
                       conf
                         .getStringList("validators")
                         .asScala
                         .map(HexString.decode)
                         .map(_.bytes)
                         .map(NodeID.apply): _*)

    val quorumsConfig = config.getConfig("core-node.quorums")
    if (quorumsConfig.hasPath("innerSets")) {
      // nest
      val simple = configToSimpleQuorumSet(quorumsConfig)
      val innerSets = quorumsConfig.getConfigList("innerSets").asScala
      if (innerSets.nonEmpty) {
        innerSets.foldLeft(simple) {(acc, n) =>
          val qs = configToSimpleQuorumSet(n).asInstanceOf[QuorumSet.Simple]
          acc.nest(qs.threshold, qs.validators.toSeq: _*)
        }
      }
      else simple
    } else {
      // simple
      configToSimpleQuorumSet(quorumsConfig)
    }
  }

}
