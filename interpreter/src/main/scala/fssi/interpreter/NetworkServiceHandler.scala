package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types.DataPacket.SubmitTransaction
import fssi.ast.domain.types._
import fssi.interpreter.util._
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.{Address, Message}
import org.slf4j.{Logger, LoggerFactory}

import scala.util._

class NetworkServiceHandler extends NetworkService.Handler[Stack] {
  val clusterOnce: Once[Cluster] = Once.empty
  val logger: Logger             = LoggerFactory.getLogger(getClass)

  override def createNode(accountPublicKey: String,
                          port: Int,
                          ip: String,
                          seeds: Vector[String]): Stack[Node] = Stack {
    Node(Node.Address(ip, port),
         Node.Type.Nymph,
         BytesValue.decodeHex(accountPublicKey),
         BytesValue.Empty,
         seeds)
  }

  override def startupP2PNode(node: Node, handler: DataPacket => Unit = _ => ()): Stack[Node] =
    Stack {
      val config = ClusterConfig
        .builder()
        .port(node.address.port)
        .listenAddress(node.address.ip)
        .portAutoIncrement(false)
        .seedMembers(node.seeds.map(Address.from): _*)
        .suspicionMult(ClusterConfig.DEFAULT_WAN_SUSPICION_MULT)
        .build()

      clusterOnce := Cluster.joinAwait(config)

      if (node.seeds.isEmpty) // as seed
        logger.info(s"P2P node started as a seed node of ID = ${node.id.value}")
      else
        logger.info(s"P2P node started, ID = ${node.id.value}")

      printMembers()
      clusterOnce foreach { cluster =>
        cluster.listenMembership().subscribe { membershipEvent =>
          logger.info(
            s"P2P Membership Event: ${membershipEvent.`type`()}, ${membershipEvent.member()}")
          printMembers()
        }

        def subscription: Message => Unit = {message =>
          val uuid = message.header("uuid")
          val timestamp = message.header("timestamp")
          logger.info(s"NOTION: recv gossip message: {uuid=$uuid, timestamp=$timestamp}")
          Try {
            val dataPacket = message.data[DataPacket]()
            handler(dataPacket)
          } match {
            case Success(_) =>
              logger.info(s"peer message handled successfully")
            case Failure(t) =>
              logger.error(s"peer message handled failed", t)
          }
        }

        // listen gossip
        cluster.listenGossips().subscribe {message =>
          logger.info("handling gossip message")
          subscription(message)
        }

        //listen message
        cluster.listen().subscribe { message =>
          logger.info("handling member message")
          subscription(message)
        }
        ()
      }

      val currentMember = clusterOnce.unsafe().member()
      node.copy(runtimeId = Some(Node.ID(currentMember.id())),
                address =
                  Node.Address(currentMember.address().host(), currentMember.address().port()))

    }

  override def shutdownP2PNode(): Stack[Unit] = Stack {
    clusterOnce.foreach { x =>
      x.shutdown().get()
      ()
    }
  }

  override def warriorNodesOfNymph(nymphNode: Node): Stack[Vector[Node.Address]] = Stack {
    setting =>
      setting.warriorNodesOfNymph
  }

  override def buildCreateAccountDataMessage(account: Account): Stack[DataPacket] = Stack {
    //io.scalecube.transport.Message
    DataPacket.CreateAccount(account)
  }

  override def buildSubmitTransactionMessage(transaction: Transaction): Stack[DataPacket] = Stack {
    SubmitTransaction(transaction)
  }

  override def buildSyncAccountMessage(id: Account.ID): Stack[DataPacket] = Stack {
    DataPacket.SyncAccount(id)
  }

  override def disseminate(packet: DataPacket, nodes: Vector[Node.Address]): Stack[Unit] = Stack {
    clusterOnce.foreach { cluster =>
      logger.info(s"disseminating to ${nodes.length} targets")
      val message = DataPacketUtil.toMessage(packet)
      nodes.foreach { addr =>
        cluster.send(Address.create(addr.ip, addr.port), message)
        logger.info(s"disseminate message to $addr")
      }
    }
  }

  override def broadcast(packet: DataPacket): Stack[Unit] = Stack {
    clusterOnce.foreach { cluster =>
      val message = DataPacketUtil.toMessage(packet)
      logger.info(s"NOTION: sending gossip message: ${message.headers()}")
      cluster.spreadGossip(DataPacketUtil.toMessage(packet))
      ()
    }
  }

  private def printMembers(): Unit = {
    logger.info("current members:")
    import scala.collection.JavaConverters._
    clusterOnce.unsafe().members().asScala.foreach(member => logger.info(s"- $member"))
  }
}
object NetworkServiceHandler {
  private val instance = new NetworkServiceHandler
  trait Implicits {
    implicit val networkServiceHandler: NetworkServiceHandler = instance
  }
  object implicits extends Implicits
}
