package fssi.ast.domain

import bigknife.sop._
import bigknife.sop.implicits._
import bigknife.sop.macros._
import fssi.ast.domain.exceptions.WorldStatesError
import fssi.ast.domain.types.Contract.Parameter
import fssi.ast.domain.types._
import fssi.contract.{AccountState, States}

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

  /**
    * save account states to the ledger
    * @param states account states, accountId -> account state
    * @return
    */
  def saveStates(states: Map[String, AccountState]): P[F, Unit]


  /**
    * get current block chain height(length, or slotIndex in scp)
    * @return
    */
  def currentHeight(): P[F, BigInt]

  /**
    * update current height
    * @param height current height
    * @return
    */
  def updateHeight(height: BigInt): P[F, Unit]

  /**
    * get the time capsule at specified height
    * @param height height, slotIndex
    * @return if not found , return moment of 0
    */
  def timeCapsuleOf(height: BigInt): P[F, TimeCapsule]

  /**
    * save time capsule
    * @param timeCapsule new time capsule
    * @return
    */
  def saveTimeCapsule(timeCapsule: TimeCapsule): P[F, Unit]


}
