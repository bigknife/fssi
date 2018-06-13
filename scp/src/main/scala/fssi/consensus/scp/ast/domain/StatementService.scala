package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._
import fssi.consensus.scp.ast.domain.types.Statement._

@sp trait StatementService[F[_]] {
  /**
    * create a nominate statement based on a slot
    * @param slot slot to create statement
    * @return
    */
  def createNomination(slot: Slot): P[F, Nominate]
  /**
    * compare two nomination statement, to check which one is newer.
    * @param a1 nomination 1
    * @param a2 nomination 2
    * @return the newer one
    */
  def newerNomination(a1: NominationStatement, a2: NominationStatement): P[F, NominationStatement]

  /**
    * check the statement is sane of not
    * @param statement statement
    * @return
    */
  def isSane(statement: Statement): P[F, Boolean]

  /**
    * create an initialized historical statement
    * @param statement statement
    * @return
    */
  def createHistoricalStatement(statement: Statement): P[F, Statement.HistoricalStatement]

}