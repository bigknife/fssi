package fssi
package interpreter
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, TimeUnit}

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

class NetworkHandler extends Network.Handler[Stack] with LogSupport {

  val consensusOnce: Once[Cluster]   = Once.empty
  val applicationOnce: Once[Cluster] = Once.empty

  val transactionOnce: Once[Map[String, Transaction]] = Once(Map.empty)

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
                  //cluster.spreadGossip(CubeMessage.fromData(scpEnvelope.asJson.noSpaces)))
                  import scala.collection.JavaConverters._

                  val members = cluster
                    .members()
                    .asScala
                    .filterNot(
                      y =>
                        y.address().host() == x.config.consensusConfig.host && y
                          .address()
                          .port() == x.config.consensusConfig.port)
                  members.foreach { m =>
                    //log.error(s"sending to ${m.address()}")
                    SCPThreadPool.broadcast(new Runnable {
                      override def run(): Unit = {
                        try {
                          val msg    = CubeMessage.fromData(scpEnvelope.asJson.noSpaces)
                          val future = new CompletableFuture[Void]
                          cluster.send(m, msg, future)
                          log.error(s"seding to ${m.address()}")
                          future.get(x.config.consensusConfig.broadcastTimeout,
                                     TimeUnit.MILLISECONDS)
                          log.error(s"sent to ${m.address()}")
                        } catch {
                          case e =>
                            log.error(
                              s"sending consensus message to ${m.address().host()}:${m.address().port()} timeout",
                              e)
                        }

                      }
                    })

                  }
                }
            }
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
        .pingInterval(30 * 60 * 1000)
        .pingTimeout(20 * 60 * 1000)
        .build()

    clusterOnce := Cluster.joinAwait(config)
    clusterOnce.foreach { cluster =>
      cluster.listenMembership().subscribe { membershipEvent =>
        log.error(
          s"receive P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
        printMembers(clusterOnce, memberTag)
      }

      val subscription: CubeMessage => Unit = { gossip =>
        scala.util.Try {
          val message = converter(gossip)
          val ts      = System.currentTimeMillis()
          message match {
            case SCPEnvelope(
                fssi.scp.types.Envelope(fssi.scp.types.Statement(from, slotIndex, _, _, x), _)) =>
              x match {
                case x0: fssi.scp.types.Message.Nomination =>
                  log.error(
                    s"GOT: nom, $from -> voted ${x0.voted.size}, accepted ${x0.accepted.size}")
                case x0: fssi.scp.types.Message.Prepare =>
                  log.error(s"GOT: prepare, $from ->  b.c=${x0.b.counter}, p'.c=${x0.`p'`
                    .map(_.counter)}, p.c=${x0.p.map(_.counter)}, c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
                case x0: fssi.scp.types.Message.Confirm =>
                  log.error(
                    s"GOT: confirm, $from -> b.c=${x0.b.counter}, p.n=${x0.`p.n`}, c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
                case x0: fssi.scp.types.Message.Externalize =>
                  log.error(s"GOT: externalize, $from -> c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
              }

            case _ =>
          }

          handler(message)

          message match {
            case SCPEnvelope(
                fssi.scp.types.Envelope(fssi.scp.types.Statement(from, slotIndex, _, _, x), _)) =>
              x match {
                case x0: fssi.scp.types.Message.Nomination =>
                  log.error(
                    s"HANDLED: nom, ${System.currentTimeMillis() - ts} ms $from -> voted ${x0.voted.size}, accepted ${x0.accepted.size}")
                case x0: fssi.scp.types.Message.Prepare =>
                  log.error(s"HANDLED: prepare, ${System
                    .currentTimeMillis() - ts} ms $from ->  b.c=${x0.b.counter}, p'.c=${x0.`p'`
                    .map(_.counter)}, p.c=${x0.p.map(_.counter)}, c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
                case x0: fssi.scp.types.Message.Confirm =>
                  log.error(s"HANDLED: confirm, ${System
                    .currentTimeMillis() - ts} ms $from -> b.c=${x0.b.counter}, p.n=${x0.`p.n`}, c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
                case x0: fssi.scp.types.Message.Externalize =>
                  log.error(s"HANDLED: externalize, ${System
                    .currentTimeMillis() - ts} ms $from -> c.n=${x0.`c.n`}, h.n=${x0.`h.n`}")
              }

            case _ =>
          }

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
          SCPThreadPool.submit(new Runnable {
            override def run(): Unit = subscription(gossip)
          })
        }

      cluster
        .listen()
        .subscribe { gossip =>
          // gossip to ApplicationMessage, ConsensusMessage
          val msg = converter(gossip)
          msg match {
            case x: ApplicationMessage =>
              //todo tuning
              handler(msg)

            case x: SCPEnvelope =>
              val nodeId = x.value.statement.from
              EnvelopePool.put(x)

              EnvelopePool.getUnworkingNom(nodeId).foreach { x =>
                EnvelopePool.setWorkingNom(nodeId, x)
                //Portal.handleEnvelope(x.value, previousValue)
                SCPThreadPool.submit(new Runnable {
                  override def run(): Unit = {
                    handler(msg)
                    EnvelopePool.endWorkingNom(nodeId, x)
                  }
                })

              }

              EnvelopePool.getUnworkingBallot(nodeId).foreach { x =>
                EnvelopePool.setWorkingBallot(nodeId, x)
                //Portal.handleEnvelope(x.value, previousValue)
                SCPThreadPool.submit(new Runnable {
                  override def run(): Unit = {
                    handler(msg)
                    EnvelopePool.endWorkingBallot(nodeId, x)
                  }
                })
              }
          }

          /*
          log.error(s"gossip got message")
          SCPThreadPool.submit(new Runnable {
            override def run(): Unit = {
              log.error("gossip message handling in SCPThreadPool")
              subscription(gossip)
              log.error("gossip message handled in SCPThreadPool")
            }
          })
          log.error("gossip message to SCPThreadPool")
         */
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
