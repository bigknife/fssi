package fssi.ast.domain

import bigknife.sop._,implicits._,macros._
import fssi.ast.domain.types._
import fssi.contract.States

@sp trait LedgerStore[F[_]] {
  /**
    * commit the moments in the proposal, then all things determinedly happened.
    * @param proposal agreed proposal, including some moments
    * @return
    */
  def commit(proposal: Proposal): P[F, Unit]

  /** load current transaction relative world states
    *
    * @param transaction transaction
    * @return
    */
  def loadStates(transaction: Transaction): P[F, States]
}
