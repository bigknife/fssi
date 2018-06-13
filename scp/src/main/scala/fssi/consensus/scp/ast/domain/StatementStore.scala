package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait StatementStore[F[_]] {
  /**
    * find latest nomination statement of a node.
    * @param nodeID node id
    * @return
    */
  def findLatestNomination(nodeID: Node.ID): P[F, Option[Statement.Nominate]]

  /**
    * update the latest nomination of a node. if not found, insert it.
    * @param nodeID node id
    * @param nominate nomination statement
    * @return
    */
  def updateLatestNomination(nodeID: Node.ID, nominate: Statement.Nominate): P[F, Unit]

  /**
    * current envelopes of all nodes
    * @return
    */
  def latestNominations(): P[F, Map[Node.ID, Envelope]]
}
