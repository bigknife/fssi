package fssi.ast.domain

import bigknife.sop._, implicits._, macros._
import fssi.ast.domain.types._

@sp trait ContractStore[F[_]] {
  /** init contract store */
  def init(): P[F, Unit]

  /** find contract */
  def findContract(name: Contract.Name, version: Contract.Version): P[F, Option[Contract]]

  /**
    * save contract
    * @param contract contract
    * @return
    */
  def saveContract(contract: Contract): P[F, Unit]
}
