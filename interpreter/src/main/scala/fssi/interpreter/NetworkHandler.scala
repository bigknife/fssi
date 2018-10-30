package fssi
package interpreter
import fssi.ast.Network
import fssi.types.biz.Node.{ApplicationNode, ConsensusNode, ServiceNode}
import fssi.types.biz._
import fssi.types.{ApplicationMessage, ClientMessage, ConsensusMessage}
import fssi.utils.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message => CubeMessage}

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val clusterOnce: Once[Cluster] = Once.empty

  override def startupConsensusNode(
      conf: ChainConfiguration,
      handler: Message.Handler[ConsensusMessage]): Stack[ConsensusNode] = Stack {
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

      val subscription: CubeMessage => Unit = { gossip =>
        scala.util.Try {
          val consensusMessage = gossip.data[ConsensusMessage]()
          log.debug("start to handle json message by gossiping")
          handler(consensusMessage)
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
        .subscribe { gossip =>
          log.debug(s"start handle Gossip message: $gossip")
          subscription(gossip)
        }
      ()
    }

    val node = Node(Node.Addr(conf.host, conf.port), conf.account)
    log.info(s"node ($node) started ")
    ConsensusNode(node)
  }

  override def startupApplicationNode(
      conf: ChainConfiguration,
      handler: Message.Handler[ApplicationMessage]): Stack[ApplicationNode] = ???

  override def startupServiceNode(conf: ChainConfiguration,
                                  handler: Message.Handler[ClientMessage]): Stack[ServiceNode] = ???

  override def shutdownConsensusNode(node: ConsensusNode): Stack[Unit] = Stack {
    clusterOnce.foreach { cluster =>
      cluster.shutdown().get()
      log.info(s"node ${cluster.address()} shutdown")
    }
  }
  override def shutdownApplicationNode(node: ApplicationNode): Stack[Unit] = ???

  override def shutdownServiceNode(node: ServiceNode): Stack[Unit] = ???

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
