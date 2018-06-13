package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait EnvelopeService[F[_]] {

  /**
    * validate signature for an envelope
    * @param envelope envelope
    * @return
    */
  def validateEnvelope(envelope: Envelope): P[F, Boolean]

  /**
    * make a signature for an envelope
    * @param envelope envelope without signature
    * @return envelope with signature
    */
  def signEnvelope(envelope: Envelope): P[F, Envelope]
}
