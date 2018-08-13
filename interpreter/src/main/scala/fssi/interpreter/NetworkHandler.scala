package fssi
package interpreter

import utils._
import types._
import ast._

import io.scalecube.cluster._
import io.scalecube.transport._
import org.slf4j._
import java.io._

class NetworkHandler extends Network.Handler[Stack] {
  val clusterOnce: Once[Cluster]  = Once.empty
  val clientOnce: Once[Transport] = Once.empty

  val logger: Logger = LoggerFactory.getLogger(getClass)

  /** startup p2p node
    */
  override def startupP2PNode(handler: JsonMessageHandler): Stack[Node] = Stack { setting =>
    setting match {
      case x: Setting.P2PNodeSetting =>
        x.workingDir.mkdirs()

        val configReader = ConfigReader(x.configFile)

        val host = configReader.readHost()
        val port = configReader.readPort()

        val config = ClusterConfig
          .builder()
          .port(port)
          .listenAddress(host)
          .portAutoIncrement(false)
          .seedMembers(
            configReader
              .readSeeds()
              .map(_.toString)
              .map(x => Node.parseAddr(x))
              .filter(_.isDefined)
              .map(_.get)
              .map(x => Address.create(x.host, x.port)): _*)
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

          val subscription: Message => Unit = { gossip =>
            scala.util.Try {
              val jsonMessage = gossip.data[JsonMessage]()
              logger.debug("start to handle json message by gossiping")
              handler.handle(jsonMessage)
              logger.debug("handled json message by gossiping")
            } match {
              case scala.util.Success(_) =>
                logger.info(s"Gossip message handled successfully")
              case scala.util.Failure(t) =>
                logger.error(s"Gossip message handled failed", t)
            }
          }

          //handle JsonMessage only
          cluster
            .listenGossips()
            .filter({ message =>
              val s = scala.util.Try {
                val jsonMessage = message.data[JsonMessage]
                !handler.ignored(jsonMessage)
              }.toOption
              if (s.isDefined) s.get else false
            })
            .subscribe { gossip =>
              logger.debug(s"start to handle Gossip message: $gossip")
              subscription(gossip)
            }

          ()
        }

        val node = Node(Node.Addr(host, port), None)

        logger.info(s"node(${node.address}) startup.")
        node

      case _ => throw new RuntimeException("CoreNodesetting needed here!")
    }
  }

  override def bindAccount(node: Node): Stack[Node] = Stack { setting =>
    setting match {
      case x: Setting.P2PNodeSetting =>
        // Read account in the config file.
        x.workingDir.mkdirs()
        val configReader = ConfigReader(x.configFile)
        val account      = configReader.readCoreNodeAccount()
        node.copy(account = Some(account))

      case _ => throw new RuntimeException("CoreNodesetting needed here!")
    }

  }

  /** shutdown current node
    */
  override def shutdownP2PNode(node: Node): Stack[Unit] = Stack { setting =>
    clusterOnce foreach { cluster =>
      cluster.shutdown().get()
      logger.info(s"node(${node.address}) shutdown.")
    }
  }

  override def broadcastMessage(message: JsonMessage): Stack[Unit] = Stack { setting =>
    clusterOnce foreach { cluster =>
      cluster.spreadGossip(Message.fromData(message))
      ()
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
