package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Node

/**
  * P2P network staff
  */
trait P2PUseCases[F[_]] {

  /** start a p2p node, connect to some seed nodes
    * if there are no seed nodes, start this node as a seed.
    * @param seeds seed nodes info, format: 'ip:port'
    */
  def startNewNode(ip: String, port: Int, seeds: Vector[String]): SP[F, Node]

  /** start a p2p seed node */
  def startSeedNode(ip: String, port: Int): SP[F, Node] =
    startNewNode(ip, port, Vector.empty)

  /** shutdown p2p node */
  def shutdown(): SP[F, Unit]

}
