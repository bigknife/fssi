package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.types.{Proposal, _}

trait WarriorUseCases[F[_]] extends P2PUseCases[F]{
  /**
    * uc1. handle message from Nymph
    *     transaction -> contract -> moment
    */
  def processTransaction(transaction: Transaction): SP[F, Transaction.Status]

  /**
    * uc2. run consensus when the proposal pool is full or time is up.
    */
  def validateProposal(): SP[F, Unit]
}
