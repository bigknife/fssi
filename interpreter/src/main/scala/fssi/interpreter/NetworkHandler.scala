package fssi
package interpreter
import fssi.ast.Network
import fssi.types.biz.{ChainConfiguration, JsonMessage, JsonMessageHandler, Node}
import fssi.utils.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message}

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val clusterOnce: Once[Cluster] = Once.empty

  override def startupPeerNode(conf: ChainConfiguration, handler: JsonMessageHandler): Stack[Node] =
    Stack {
      val config =
        ClusterConfig
          .builder()
          .listenAddress(conf.host)
          .port(conf.port)
          .portAutoIncrement(false)
          .seedMembers(conf.seeds.map(x => Address.create(x.host, x.port)): _*)
          .suspicionMult(ClusterConfig.DEFAULT_WAN_SUSPICION_MULT)
          .build()

      clusterOnce := Cluster.joinAwait(config)
      clusterOnce.foreach { cluster =>
        cluster.listenMembership().subscribe { membershipEvent =>
          log.info(
            s"receive P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
          printMembers()
        }

        val subscription: Message => Unit = { gossip =>
          scala.util.Try {
            val jsonMessage = gossip.data[JsonMessage]()
            log.debug("start to handle json message by gossiping")
            handler.handle(jsonMessage)
            log.debug("handled json message by gossiping")
          } match {
            case scala.util.Success(_) =>
              log.debug("Gossip message handled successful")
            case scala.util.Failure(e) =>
              log.error("Gossip message handled failed", e)
          }
        }

        cluster
          .listenGossips()
          .filter({ message =>
            scala.util
              .Try {
                val jsonMessage = message.data[JsonMessage]
                !handler.ignored(jsonMessage)
              }
              .toOption
              .getOrElse(false): Boolean
          })
          .subscribe { gossip =>
            log.debug(s"start handle Gossip message: $gossip")
            subscription(gossip)
          }
      }

      val node = Node(Node.Addr(conf.host, conf.port), conf.account)
      log.info(s"node ($node) started ")
      node
    }

  override def shutdown(): Stack[Unit] = Stack {
    clusterOnce.foreach { cluster =>
      cluster.shutdown().get()
      log.info(s"node ${cluster.address()} shutdown")
    }
  }

  private def printMembers(): Unit = {
    log.info("current members")
    import scala.collection.JavaConverters._
    clusterOnce.unsafe().members().asScala.foreach(member => log.info(s"-- $member --"))
  }
}

object NetworkHandler {
  val instance = new NetworkHandler

  trait Implicits {
    implicit val networkHandler: NetworkHandler = instance
  }
}
