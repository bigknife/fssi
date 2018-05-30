package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

@sp trait ContractService[F[_]] {
  /** resolve a transaction to contract */
  def resolveTransaction(transaction: Transaction): P[F, (Contract.Name, Contract.Version)]

  /** run the contract */
  def runContract(invoker: Account, contract: Contract): P[F, Either[Throwable, Moment]]
}
