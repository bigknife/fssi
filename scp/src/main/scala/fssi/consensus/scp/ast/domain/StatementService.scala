package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types.Statement.NominationStatement

trait StatementService[F[_]] {
  /**
    * compare two nomination statement, to check which one is newer.
    * @param a1 nomination 1
    * @param a2 nomination 2
    * @return the newer one
    */
  def newerNomination(a1: NominationStatement, a2: NominationStatement): P[F, NominationStatement]
}