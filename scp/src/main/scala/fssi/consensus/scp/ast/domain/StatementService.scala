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
  def newerNominationStatement(a1: NominationStatement, a2: NominationStatement): P[F, NominationStatement]

  /**
    * compare two ballot statement, to check which one is newer.
    * the order principles is defined in the stellar paper.
    * @param a1 a ballot statement
    * @param a2 the other one statement
    * @return
    */
  def newerBallotStatement(a1: BallotStatement, a2: BallotStatement): P[F, BallotStatement]

  /**
    * check the statement is sane of not
    * @param statement statement
    * @return
    */
  def isSaneNominationStatement(statement: Statement.NominationStatement): P[F, Boolean]

  /**
    * create an initialized historical statement
    * @param statement statement
    * @return
    */
  def createHistoricalStatement(statement: Statement): P[F, Statement.HistoricalStatement]

  /** is a sane statement for ballot protocol */
  def isSaneBallotStatement(statement: Statement.BallotStatement): P[F, Boolean]

  /** build a set of value from a ballot statement to validate */
  def valuesOfStatementToValidate(statement: Statement.BallotStatement): P[F, Set[Value]]

  /** get a working ballot from ballot statement */
  def getWorkingBallot(statement: Statement.BallotStatement): P[F, Ballot]

  /** get prepared candidate ballots of a ballot statement */
  def getPrepareCandidates(statement: Statement.BallotStatement): P[F, Set[Ballot]]
}