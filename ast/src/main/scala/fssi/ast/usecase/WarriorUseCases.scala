package fssi.ast.usecase

import bigknife.sop._
import fssi.ast.domain.Proposal
import fssi.ast.domain.types._

trait WarriorUseCases[F[_]] {
  /**
    * uc1. handle message from Nymph
    */
  def processTransaction(transaction: Transaction): SP[F, Transaction.Status]
}
