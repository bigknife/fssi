package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

@sp trait LedgerStore[F[_]] {
  /**
    * commit the moments in the proposal, then all things determinedly happened.
    * @param proposal agreed proposal, including some moments
    * @return
    */
  def commit(proposal: Proposal): P[F, Unit]
}
