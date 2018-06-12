package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait EnvelopeService[F[_]] {
  /**
    * validate an envelope
    * @param envelope envelope
    * @return
    */
  def validateEnvelope(envelope: Envelope): P[F, Boolean]
}
