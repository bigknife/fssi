package fssi
package interpreter
import fssi.ast.Network
import fssi.interpreter.Configuration.P2PConfig
import fssi.interpreter.Setting.{CoreNodeSetting, EdgeNodeSetting}
import fssi.types.biz.Node.{ApplicationNode, ConsensusNode, ServiceNode}
import fssi.types.biz._
import fssi.types.{ApplicationMessage, ClientMessage, ConsensusMessage, ServiceResource}
import fssi.utils.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message => CubeMessage}

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val consensusOnce: Once[Cluster]   = Once.empty
  val applicationOnce: Once[Cluster] = Once.empty
  val serviceOnce: Once[Cluster]     = Once.empty

  override def startupConsensusNode(
      conf: ChainConfiguration,
      handler: Message.Handler[ConsensusMessage]): Stack[ConsensusNode] = Stack { setting =>
    val p2pConfig = setting match {
      case coreNodeSetting: CoreNodeSetting => coreNodeSetting.config.consensusConfig
      case _                                => throw new RuntimeException("unsupported setting to startup consensus")
    }
    val converter: CubeMessage => ConsensusMessage = cub => cub.data[ConsensusMessage]
    val node =
      startP2PNode(consensusOnce, p2pConfig, converter, handler)
    ConsensusNode(node)
  }

  override def startupApplicationNode(
      conf: ChainConfiguration,
      handler: Message.Handler[ApplicationMessage]): Stack[ApplicationNode] = Stack { setting =>
    val applicationConfig = setting match {
      case coreNodeSetting: CoreNodeSetting => coreNodeSetting.config.applicationConfig
      case edgeNodeSetting: EdgeNodeSetting => edgeNodeSetting.config.applicationConfig
    }
    val converter: CubeMessage => ApplicationMessage = cube => cube.data[ApplicationMessage]
    val node                                         = startP2PNode(applicationOnce, applicationConfig, converter, handler)
    ApplicationNode(node)
  }

  override def startupServiceNode(conf: ChainConfiguration,
                                  handler: Message.Handler[ClientMessage],
                                  serviceResource: ServiceResource): Stack[ServiceNode] = Stack {
    setting =>
      setting match {
        case edgeNodeSetting: EdgeNodeSetting =>
          serviceResource()
          val config  = edgeNodeSetting.config.jsonRPCConfig
          val address = Node.Addr(config.host, config.port)
          /// TODO: cope service node account
          val account = edgeNodeSetting.config.applicationConfig.account
          ServiceNode(Node(address, account))
        case _ =>
          throw new RuntimeException("unsupported edge node setting to startup service node")
      }
  }

  override def shutdownConsensusNode(node: ConsensusNode): Stack[Unit] = Stack {
    shutdownP2PNode(consensusOnce)
  }
  override def shutdownApplicationNode(node: ApplicationNode): Stack[Unit] = Stack {
    shutdownP2PNode(applicationOnce)
  }

  override def shutdownServiceNode(node: ServiceNode): Stack[Unit] = ???

  private def startP2PNode[M <: Message](clusterOnce: Once[Cluster],
                                         p2pConfig: P2PConfig,
                                         converter: CubeMessage => M,
                                         handler: Message.Handler[M]): Node = {
    val config =
      ClusterConfig
        .builder()
        .listenAddress(p2pConfig.host)
        .port(p2pConfig.port)
        .portAutoIncrement(false)
        .seedMembers(p2pConfig.seeds.map(x => Address.create(x.host, x.port)): _*)
        .suspicionMult(ClusterConfig.DEFAULT_WAN_SUSPICION_MULT)
        .build()

    clusterOnce := Cluster.joinAwait(config)
    clusterOnce.foreach { cluster =>
      cluster.listenMembership().subscribe { membershipEvent =>
        log.info(
          s"receive P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
        printMembers(clusterOnce)
      }

      val subscription: CubeMessage => Unit = { gossip =>
        scala.util.Try {
          val message = converter(gossip)
          log.debug("start to handle json message by gossiping")
          handler(message)
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

    val node = Node(Node.Addr(p2pConfig.host, p2pConfig.port), p2pConfig.account)
    log.info(s"node ($node) started ")
    node
  }

  private def shutdownP2PNode(clusterOnce: Once[Cluster]): Unit = {
    clusterOnce.foreach { cluster =>
      cluster.shutdown().get()
      log.info(s"node ${cluster.address()} shutdown")
    }
  }

  private def printMembers(clusterOnce: Once[Cluster]): Unit = {
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
