package fssi.consensus.scp.ast.usecase

import bigknife.sop._
import fssi.consensus.scp.ast.domain.types._


trait SCPUseCases[F[_]] {
  /**
    * called when node accept an envelope of message.
    * @param envelope message envelope
    * @return handling state
    */
  def handleEnvelope(envelope: Envelope): SP[F, Envelope.State]
}
