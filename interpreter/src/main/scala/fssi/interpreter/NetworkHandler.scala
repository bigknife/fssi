package fssi
package interpreter

import util._
import types._
import ast._

import io.scalecube.cluster._
import io.scalecube.transport._
import org.slf4j._
import java.io._

class NetworkHandler extends Network.Handler[Stack] {
  val clusterOnce: Once[Cluster] = Once.empty
  val logger: Logger             = LoggerFactory.getLogger(getClass)


  /** startup p2p node
    */
  override def startup(handler: JsonMessageHandler): Stack[Node] = Stack { setting =>
    setting match {
      case x: Setting.CoreNodeSetting =>
        val configReader = ConfigReader(new File(x.workingDir, "core-node.conf"))

        val host = configReader.readHost()
        val port = configReader.readPort()

        val config = ClusterConfig
          .builder()
          .port(port)
          .listenAddress(host)
          .portAutoIncrement(false)
          .seedMembers(configReader.readSeeds().map(_.toString).map(Address.from): _*)
          .suspicionMult(ClusterConfig.DEFAULT_WAN_SUSPICION_MULT)
          .build()
        clusterOnce := Cluster.joinAwait(config)
        clusterOnce.foreach { cluster =>
          // print members
          cluster.listenMembership().subscribe { membershipEvent =>
            logger.info(
              s"P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
            printMembers()
          }

          //handle JsonMessage only
          cluster.listenGossips().subscribe { gossip =>
            logger.debug(s"start to handle Gossip message: $gossip")
            scala.util.Try {
              val jsonMessage = gossip.data[JsonMessage]()
              logger.debug("start to handle json message by gossiping")
              handler(jsonMessage)
              logger.debug("handled json message by gossiping")
            } match {
              case scala.util.Success(_) =>
                logger.info(s"Gossip message handled successfully")
              case scala.util.Failure(t) =>
                logger.error(s"Gossip message handled failed", t)
            }
          }
          ()
        }

        Node(Node.Addr(host, port), None)

      case _ => throw new RuntimeException("CoreNodesetting needed here!")
    }
  }

  override def bindAccount(node: Node): Stack[Node] = Stack { setting =>
    setting match {
      case x: Setting.CoreNodeSetting =>
        //todo: read account from config file, the decrypt the private key
        node
      case _                          => throw new RuntimeException("CoreNodesetting needed here!")
    }

  }

  private def printMembers(): Unit = {
    logger.info("current members:")
    import scala.collection.JavaConverters._
    clusterOnce.unsafe().members().asScala.foreach(member => logger.info(s"- $member"))
  }
}

object NetworkHandler {
  val instance = new NetworkHandler

  trait Implicits {
    implicit val networkHandlerInstance: NetworkHandler = instance
  }
}
