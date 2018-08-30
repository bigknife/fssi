package fssi
package interpreter

import bigknife.scalap.ast.types._
import types._
import com.typesafe.config._

import scala.collection.JavaConverters._

/** read fssi.conf
  * @see config-sample.conf
  */
case class ConfigReader(conf: java.io.File) {
  lazy val config: Config = ConfigFactory.parseFile(conf)

  object p2p {
    lazy val host: String = config.getString("p2p.host")
    lazy val port: Int    = config.getInt("p2p.port")
    lazy val seeds: Vector[Node.Addr] =
      config
        .getStringList("p2p.seeds")
        .asScala
        .toVector
        .map(Node.parseAddr)
        .filter(_.isDefined)
        .map(_.get)
    lazy val bindAccount: Account = Account(
      publicKey = HexString.decode(config.getString("p2p.account.publicKey")),
      encryptedPrivateKey = HexString.decode(config.getString("p2p.account.encryptedPrivateKey")),
      iv = HexString.decode(config.getString("p2p.account.iv"))
    )
  }

  object edgeNode {
    object jsonRpc {
      lazy val port: Int    = config.getInt("edge-node.jsonrpc.port")
      lazy val host: String = config.getString("edge-node.jsonrpc.host")
    }
  }

  object coreNode {
    object scp {
      lazy val quorumSet: QuorumSet = {
        def configToSimpleQuorumSet(conf: Config): QuorumSet =
          QuorumSet.simple(conf.getInt("threshold"),
                           conf
                             .getStringList("validators")
                             .asScala
                             .map(HexString.decode)
                             .map(_.bytes)
                             .map(NodeID.apply): _*)

        val quorumsConfig = config.getConfig("core-node.scp.quorums")
        if (quorumsConfig.hasPath("innerSets")) {
          // nest
          val simple    = configToSimpleQuorumSet(quorumsConfig)
          val innerSets = quorumsConfig.getConfigList("innerSets").asScala
          if (innerSets.nonEmpty) {
            innerSets.foldLeft(simple) { (acc, n) =>
              val qs = configToSimpleQuorumSet(n).asInstanceOf[QuorumSet.Simple]
              acc.nest(qs.threshold, qs.validators.toSeq: _*)
            }
          } else simple
        } else {
          // simple
          configToSimpleQuorumSet(quorumsConfig)
        }
      }
      lazy val maxTimeoutSeconds: Int = config.getInt("core-node.scp.maxTimeoutSeconds")
      lazy val maxNominatingTimes: Int = config.getInt("core-node.scp.maxNominatingTimes")
    }
  }
}
