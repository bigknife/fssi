package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Node
import fssi.ast.domain.types.DataPacket

/**
  * P2P network staff
  */
trait P2PUseCases[F[_]] {

  /** start a p2p node, connect to some seed nodes
    * if there are no seed nodes, start this node as a seed.
    * @param seeds seed nodes info, format: 'ip:port'
    */
  def startup(accountPublicKey: String,
              ip: String,
              port: Int,
              seeds: Vector[String],
              handler: DataPacket => Unit = _ => ()): SP[F, Node]

  /** start a p2p seed node
  def startSeedNode(ip: String, port: Int): SP[F, Node] =
    startup(ip, port, Vector.empty)
    */
  /** shutdown p2p node */
  def shutdown(): SP[F, Unit]

}
