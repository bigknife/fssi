package fssi.ast.usecase
import bigknife.sop._, implicits._
import fssi.ast.domain.Node
import fssi.ast.domain.components.Model

trait P2P[F[_]] extends P2PUseCases[F] {

  val model: Model[F]
  import model._

  /** start a p2p node, connect to some seed nodes
    * if there are no seed nodes, start this node as a seed.
    */
  override def startNewNode(ip: String, port: Int, seeds: Vector[String]): SP[F, Node] = {
    for {
      node <- networkService.createNode(port, ip, seeds)
      runtimeNode    <- networkService.startupP2PNode(node)
      _    <- networkStore.saveNode(runtimeNode)
    } yield runtimeNode
  }

  override def shutdown(): SP[F, Unit] = networkService.shutdownP2PNode()
}
