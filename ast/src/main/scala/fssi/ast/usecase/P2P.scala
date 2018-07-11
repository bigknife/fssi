package fssi.ast.usecase
import bigknife.sop._
import implicits._
import fssi.ast.domain.Node
import fssi.ast.domain.components.Model
import fssi.ast.domain.types.DataPacket

trait P2P[F[_]] extends P2PUseCases[F] {

  val model: Model[F]
  import model._

  /** start a p2p node, connect to some seed nodes
    * if there are no seed nodes, start this node as a seed.
    */
  override def startup(accountPublicKey: String,
                       ip: String,
                       port: Int,
                       seeds: Vector[String],
                       handler: DataPacket => Unit = _ => ()): SP[F, Node] = {
    for {
      _           <- log.info(s"starting node(ip = $ip, port = $port, seeds = $seeds)...")
      _           <- accountSnapshot.startupSnapshotDB()
      node        <- networkService.createNode(accountPublicKey, port, ip, seeds)
      _           <- log.info(s"created node: $node")
      _           <- ledgerStore.init()
      _           <- log.info("ledger store initialized.")
      _           <- consensusEngine.init()
      _           <- log.info("censensus engine initialized.")
      runtimeNode <- networkService.startupP2PNode(node, handler)
      _           <- log.info(s"node started, runtime node: $runtimeNode")
      _           <- networkStore.saveNode(runtimeNode)
      _           <- log.info(s"runtime node $runtimeNode saved locally")
    } yield runtimeNode
  }

  override def shutdown(): SP[F, Unit] =
    for {
      runtimeNode <- networkStore.currentNode()
      _           <- log.info(s"shutting down node: $runtimeNode")
      _           <- networkService.shutdownP2PNode()
      _           <- accountSnapshot.shutdownSnapshotDB()
      _           <- log.info(s"node $runtimeNode shutdown!")
    } yield ()
}
