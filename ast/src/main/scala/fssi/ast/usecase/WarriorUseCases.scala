package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Proposal
import fssi.ast.domain.types._

trait WarriorUseCases[F[_]] {
  /**
    * uc1. handle Account-Created message from Nymph
    */
  def validateProposal(proposal: Proposal): P[F, Boolean]
}
