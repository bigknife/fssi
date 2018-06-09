package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.implicits._
import bigknife.sop.macros._
import fssi.ast.domain.exceptions.WorldStatesError
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types._
import fssi.contract.States

@sp trait LedgerStore[F[_]] {
  /**
    * init the ledger store
    * @return
    */
  def init(): P[F, Unit]

  /**
    * commit the moments in the proposal, then all things determinedly happened.
    * @param proposal agreed proposal, including some moments
    * @return
    */
  def commit(proposal: Proposal): P[F, Unit]

  /** load current contract-relative world states
    *
    * @param contract relative contract
    * @return
    */
  def loadStates(invoker: Account.ID,
                 contract: Contract,
                 parameter: Option[Parameter]): P[F, Either[WorldStatesError, States]]
}
