package fssi.ast.domain

import bigknife.sop._, implicits._, macros._
import fssi.ast.domain.types._

@sp trait ContractStore[F[_]] {

  /** find contract */
  def findContract(name: Contract.Name, version: Contract.Version): P[F, Option[Contract]]

  /**
    * save contract
    * @param name name
    * @param version version
    * @param contract contract
    * @return
    */
  def saveContract(name: Contract.Name, version: Contract.Version, contract: Contract): P[F, Unit]
}
