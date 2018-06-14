package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

trait EnvelopeStore[F[_]] {
  /**
    * find latest Envelope for a node
    * @param nodeID node id
    * @return
    */
  def findLatestEnvelope(nodeID: Node.ID): P[F, Option[Envelope]]

  /**
    * save latest envelope for a node
    * @param nodeID node id
    * @param envelope new envelope
    * @return
    */
  def saveLatestEnvelope(nodeID: Node.ID, envelope: Envelope): P[F, Unit]
}
