package fssi
package interpreter
import fssi.ast.Network
import fssi.interpreter.Configuration.P2PConfig
import fssi.interpreter.Setting.{CoreNodeSetting, EdgeNodeSetting}
import fssi.types.biz.Node.{ApplicationNode, ConsensusNode, ServiceNode}
import fssi.types.biz._
import fssi.types.{ApplicationMessage, ClientMessage, ConsensusMessage}
import fssi.utils.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message => CubeMessage}
import bigknife.jsonrpc._
import fssi.interpreter.jsonrpc.EdgeJsonRpcResource
import fssi.types.biz.Message.ApplicationMessage.{QueryMessage, TransactionMessage}
import fssi.types.biz.Message.ClientMessage.{QueryTransaction, SendTransaction}
import fssi.types.implicits._
import fssi.types.json.implicits._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val consensusOnce: Once[Cluster]   = Once.empty
  val applicationOnce: Once[Cluster] = Once.empty

  val transactionOnce: Once[Map[String, Transaction]] = Once(Map.empty)

  override def startupConsensusNode(
      handler: Message.Handler[ConsensusMessage, Unit]): Stack[ConsensusNode] = Stack { setting =>
    val p2pConfig = setting match {
      case coreNodeSetting: CoreNodeSetting => coreNodeSetting.config.consensusConfig
      case _                                => throw new RuntimeException("unsupported setting to startup consensus")
    }
    val converter: CubeMessage => ConsensusMessage = cub => cub.data[ConsensusMessage]
    val node =
      startP2PNode(consensusOnce, p2pConfig, converter, handler, "consensus node")
    ConsensusNode(node)
  }

  override def startupApplicationNode(
      handler: Message.Handler[ApplicationMessage, Unit]): Stack[ApplicationNode] = Stack {
    setting =>
      val applicationConfig = setting match {
        case coreNodeSetting: CoreNodeSetting => coreNodeSetting.config.applicationConfig
        case edgeNodeSetting: EdgeNodeSetting => edgeNodeSetting.config.applicationConfig
      }
      val converter: CubeMessage => ApplicationMessage = cube => cube.data[ApplicationMessage]
      val node =
        startP2PNode(applicationOnce, applicationConfig, converter, handler, "application node")
      ApplicationNode(node)
  }

  override def startupServiceNode(
      handler: Message.Handler[ClientMessage, Transaction]): Stack[ServiceNode] =
    Stack { setting =>
      setting match {
        case edgeNodeSetting: EdgeNodeSetting =>
          val config   = edgeNodeSetting.config.jsonRPCConfig
          val address  = Node.Addr(config.host, config.port)
          val resource = new EdgeJsonRpcResource(handler)
          server.run("edge", "v1", resource, address.port, address.host)
          log.info(
            s"edge node json rpc service startup: http://${config.host}:${config.port}/jsonrpc/edge/v1")
          val (account, _) = edgeNodeSetting.config.applicationConfig.account
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

  override def broadcastMessage(message: Message): Stack[Unit] = Stack { setting =>
    setting match {
      case _: CoreNodeSetting =>
        message match {
          case consensusMessage: ConsensusMessage =>
            consensusOnce.foreach(cluster =>
              cluster.spreadGossip(CubeMessage.fromData(consensusMessage)))
          case _ => throw new RuntimeException(s"core node unsupported broadcast message: $message")
        }
      case _: EdgeNodeSetting =>
        message match {
          case sendTransaction: SendTransaction =>
            applicationOnce.foreach(
              cluster =>
                cluster.spreadGossip(
                  CubeMessage.fromData(TransactionMessage(sendTransaction.payload))))
          case queryTransaction: QueryTransaction =>
            applicationOnce.foreach(cluster =>
              cluster.spreadGossip(CubeMessage.fromData(QueryMessage(queryTransaction.payload))))
          case _ => throw new RuntimeException(s"edge node unsupported broadcast message: $message")
        }
      case _ =>
    }
  }

  override def handledQueryTransaction(
      queryTransaction: QueryTransaction): Stack[Option[Transaction]] = Stack {
    transactionOnce.map(_.get(queryTransaction.payload.asBytesValue.utf8String)).unsafe()
  }

  override def receiveTransactionMessage(applicationMessage: ApplicationMessage): Stack[Unit] =
    Stack {
      applicationMessage match {
        case transactionMessage: TransactionMessage =>
          val jsonString = transactionMessage.payload.asBytesValue.utf8String
          parse(jsonString).right.toOption match {
            case Some(json) =>
              json.as[Transaction].right.toOption match {
                case Some(transaction) =>
                  transactionOnce.updated(
                    _ + (transaction.id.asBytesValue.utf8String -> transaction)); ()
                case None =>
                  throw new RuntimeException(
                    s"received core node application message: $jsonString can not convert to Transaction")
              }
            case None =>
              throw new RuntimeException(
                s"received core node application message: $jsonString must be a transaction json")
          }
        case _ =>
      }
      ()
    }

  private def startP2PNode[M <: Message](clusterOnce: Once[Cluster],
                                         p2pConfig: P2PConfig,
                                         converter: CubeMessage => M,
                                         handler: Message.Handler[M, Unit],
                                         memberTag: String): Node = {
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
        printMembers(clusterOnce, memberTag)
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

    val node = Node(Node.Addr(p2pConfig.host, p2pConfig.port), p2pConfig.account._1)
    log.info(s"node ($node) started ")
    node
  }

  private def shutdownP2PNode(clusterOnce: Once[Cluster]): Unit = {
    clusterOnce.foreach { cluster =>
      cluster.shutdown().get()
      log.info(s"node ${cluster.address()} shutdown")
    }
  }

  private def printMembers(clusterOnce: Once[Cluster], memberTag: String): Unit = {
    import scala.collection.JavaConverters._
    log.info(s"$memberTag current members:")
    clusterOnce.unsafe().members().asScala.foreach(member => log.info(s"-- $member --"))
  }
}

object NetworkHandler {
  val instance = new NetworkHandler

  trait Implicits {
    implicit val networkHandler: NetworkHandler = instance
  }
}
