package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types.DataPacket.SubmitTransaction
import fssi.ast.domain.types._
import fssi.interpreter.util._
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.Address
import org.slf4j.{Logger, LoggerFactory}

import scala.util._

class NetworkServiceHandler extends NetworkService.Handler[Stack] {
  val clusterOnce: Once[Cluster] = Once.empty
  val logger: Logger             = LoggerFactory.getLogger(getClass)

  override def createNode(port: Int, ip: String, seeds: Vector[String]): Stack[Node] = Stack {
    Node(Node.Address(ip, port), Node.Type.Nymph, None, seeds)
  }

  override def startupP2PNode(node: Node, handler: DataPacket => Unit = _ => ()): Stack[Node] =
    Stack {
      val config = ClusterConfig
        .builder()
        .port(node.address.port)
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
        cluster.listen().subscribe { message =>
          logger.info(s"got p2p message: $message")
          Try {
            val dataPacket = message.data[DataPacket]()
            handler(dataPacket)
          } match {
            case Success(_) =>
              logger.info(s"handled p2p message: $message")
            case Failure(t) =>
              logger.info(s"handling p2p message $message failed", t)
          }

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

  override def buildSubmitTransactionMessage(account: Account,
                                             transaction: Transaction): Stack[DataPacket] = Stack {
    SubmitTransaction(account, transaction)
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

  private def printMembers(): Unit = {
    logger.info("current members:")
    import scala.collection.JavaConverters._
    clusterOnce.unsafe().members().asScala.foreach(member => logger.info(s"- $member"))
  }
}
object NetworkServiceHandler {
  trait Implicits {
    implicit val networkServiceHandler: NetworkServiceHandler = new NetworkServiceHandler
  }
  object implicits extends Implicits
}
