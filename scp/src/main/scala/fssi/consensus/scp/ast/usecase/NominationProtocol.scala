package fssi.consensus.scp.ast.usecase

import bigknife.sop._
import fssi.consensus.scp.ast.domain.components.Model
import fssi.consensus.scp.ast.domain.types._

trait NominationProtocol[F[_]] {
  val model: Model[F]
  import model._

  def runNominationProtocol(slot: Slot, envelope: Envelope): SP[F, Envelope.State]
}
