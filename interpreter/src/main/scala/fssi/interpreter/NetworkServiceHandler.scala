package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._
import fssi.interpreter.util.Once
import io.scalecube.cluster.{Cluster, ClusterConfig}
import io.scalecube.transport.Address
import org.slf4j.{Logger, LoggerFactory}

class NetworkServiceHandler extends NetworkService.Handler[Stack] {
  val clusterOnce: Once[Cluster] = Once.empty
  val logger: Logger             = LoggerFactory.getLogger("")

  override def createNode(port: Int, ip: String, seeds: Vector[String]): Stack[Node] = Stack {
    Node(port, ip, Node.Type.Nymph, None, seeds)
  }

  override def startupP2PNode(node: Node): Stack[Node] = Stack { setting =>
    val config = ClusterConfig
      .builder()
      .port(node.port)
      .portAutoIncrement(false)
      .seedMembers(node.seeds.map(Address.from): _*)
      .build()

    clusterOnce := Cluster.joinAwait(config)

    if (node.seeds.isEmpty) // as seed
      logger.info(s"P2P node started as a seed node of ID = ${node.id.value}")
    else
      logger.info(s"P2P node started, ID = ${node.id.value}")

    printMembers()
    val currentMember = clusterOnce.unsafe().member()
    node.copy(runtimeId = Some(Node.ID(currentMember.id())),
              ip = currentMember.address().host(),
              port = currentMember.address().port())

  }

  override def shutdownP2PNode(): Stack[Unit] = Stack {
    clusterOnce.foreach { x =>
      x.shutdown().get()
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
  trait Implicits {
    implicit val networkServiceHandler: NetworkServiceHandler = new NetworkServiceHandler
  }
  object implicits extends Implicits
}
