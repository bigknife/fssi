package fssi
package interpreter
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, TimeUnit}

import fssi.ast.Network
import fssi.interpreter.Configuration.{ApplicationConfig, ConsensusConfig, P2PConfig}
import fssi.interpreter.Setting.{CoreNodeSetting, EdgeNodeSetting}
import fssi.types.biz.Node.{ApplicationNode, ConsensusNode, ServiceNode}
import fssi.types.biz._
import fssi.types.{ApplicationMessage, ClientMessage, ConsensusMessage}
import fssi.utils.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message => CubeMessage}
import bigknife.jsonrpc._
import fssi.interpreter.jsonrpc.EdgeJsonRpcResource
import fssi.interpreter.network.{MessageReceiver, MessageWorker}
import fssi.interpreter.scp.SCPEnvelope
import fssi.types.biz.Message.ApplicationMessage.{QueryMessage, TransactionMessage}
import fssi.types.biz.Message.ClientMessage.{QueryTransaction, SendTransaction}
import fssi.types.implicits._
import io.circe.syntax._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import fssi.interpreter.scp.BlockValue.implicits._
import fssi.scp.interpreter.SCPThreadPool
import fssi.scp.interpreter.json.implicits._
import fssi.types.json.implicits._
import rx.schedulers.Schedulers

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val consensusOnce: Once[Cluster]   = Once.empty
  val applicationOnce: Once[Cluster] = Once.empty

  val transactionOnce: Once[Map[String, Transaction]] = Once(Map.empty)

  val appMessageWorker: Once[AnyRef]                  = Once.empty
  val appMessageReceiver: Once[MessageReceiver]       = Once.empty
  val consensusMessageWorker: Once[AnyRef]            = Once.empty
  val consensusMessageReceiver: Once[MessageReceiver] = Once.empty

  //val executor: ExecutorService = Executors.newSingleThreadExecutor()

  override def startupConsensusNode(
      handler: Message.Handler[ConsensusMessage, Unit]): Stack[ConsensusNode] = Stack { setting =>
    val p2pConfig = setting match {
      case coreNodeSetting: CoreNodeSetting => coreNodeSetting.config.consensusConfig
      case _                                => throw new RuntimeException("unsupported setting to startup consensus")
    }
    val converter: CubeMessage => ConsensusMessage = cub => {
      val jsonString = cub.data[String]()
      val scpEither = for {
        json <- parse(jsonString)
        r    <- json.as[SCPEnvelope]
      } yield r
      scpEither match {
        case Right(envelope) => envelope
        case Left(e) =>
          throw new RuntimeException(s"consensus node can not handle message: $jsonString", e)
      }
    }
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
        case _                                => throw new RuntimeException("un matched config")
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
      case x: CoreNodeSetting =>
        message match {
          case consensusMessage: ConsensusMessage =>
            consensusMessage match {
              case scpEnvelope: SCPEnvelope =>
                consensusOnce.foreach { cluster =>
                  SCPThreadPool.broadcast(() => {
                    cluster.spreadGossip(CubeMessage.fromData(scpEnvelope.asJson.noSpaces))
                    /*
                    val msg = CubeMessage.fromData(scpEnvelope.asJson.noSpaces)
                    cluster.otherMembers().forEach { m =>
                      val future = new CompletableFuture[Void]
                      cluster.send(m, msg, future)
                    }
                    */
                  })

                }
            }
          case _ => throw new RuntimeException(s"core node unsupported broadcast message: $message")
        }
      case _: EdgeNodeSetting =>
        message match {
          case sendTransaction: SendTransaction =>
            applicationOnce.foreach { cluster =>
              cluster.spreadGossip(
                CubeMessage.fromData(TransactionMessage(sendTransaction.payload)))
              ()
            }
          case queryTransaction: QueryTransaction =>
            applicationOnce.foreach { cluster =>
              cluster.spreadGossip(CubeMessage.fromData(QueryMessage(queryTransaction.payload)))
              ()
            }
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

    clusterOnce := {
      val config =
        ClusterConfig
          .builder()
          .listenAddress(p2pConfig.host)
          .port(p2pConfig.port)
          .portAutoIncrement(false)
          .seedMembers(p2pConfig.seeds.map(x => Address.create(x.host, x.port)): _*)
          .suspicionMult(ClusterConfig.DEFAULT_WAN_SUSPICION_MULT)
          .build()
      Cluster.joinAwait(config)
    }

    p2pConfig match {
      case _: ConsensusConfig =>
        consensusMessageWorker := {
          consensusMessageReceiver := MessageReceiver()
          val x = MessageWorker(consensusMessageReceiver.unsafe(), handler)
          x.startWork()
          x
        }
      case _: ApplicationConfig =>
        appMessageWorker := {
          appMessageReceiver := MessageReceiver()
          val x = MessageWorker(appMessageReceiver.unsafe(), handler)
          x.startWork()
          x
        }
    }

    clusterOnce.foreach { cluster =>
      printMembers(clusterOnce, memberTag)
      cluster.listenMembership().subscribe { membershipEvent =>
        log.debug(s"receive P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
        printMembers(clusterOnce, memberTag)
      }
      cluster
        .listenGossips()
        .subscribe { gossip =>
          val msg = converter(gossip)
          msg match {
            case _: Message.ApplicationMessage =>
              appMessageReceiver.foreach(_.receive(msg))
            case _: Message.ConsensusMessage =>
              consensusMessageReceiver.foreach(_.receive(msg))
          }
        }

      cluster
        .listen()
        .subscribe { gossip =>
          val msg = converter(gossip)
          msg match {
            case _: Message.ApplicationMessage =>
              appMessageReceiver.foreach(_.receive(msg))
            case _: Message.ConsensusMessage =>
              consensusMessageReceiver.foreach(_.receive(msg))
          }
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
    log.info(s"==== $memberTag current members: =======================")
    clusterOnce.unsafe().members().asScala.foreach(member => log.info(s"=    -- $member --"))
    log.info(s"========================================================")
  }
}

object NetworkHandler {
  val instance = new NetworkHandler

  trait Implicits {
    implicit val networkHandler: NetworkHandler = instance
  }
}
