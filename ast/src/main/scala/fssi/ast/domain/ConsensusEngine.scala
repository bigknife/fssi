package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

@sp trait ConsensusEngine[F[_]] {
  /**
    * init engine
    * @return
    */
  def init(): P[F, Unit]
  /**
    * put the moment into the pool
    * @param moment moment
    * @return if the pool can accept it return true, or false
    */
  def poolMoment(node: Node, currentHeight: BigInt, previous: Moment, moment: Moment): P[F, Boolean]

  /**
    * build a proposal from moment pool
    * @return if time is up or pool is full, return a proposal built by using them, or none
    */
  def buildProposal(): P[F, Option[Proposal]]

  /**
    * run consensus to advance moments to final state
    * @return consensus engine validate every moment in the proposal, then put all agreed into a new proposal
    */
  def runConsensus(proposal: Proposal): P[F, Proposal]
}
