package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait QuorumSetService[F[_]] {
  /**
    * get a quorum set from a statement
    * @param statement statement
    * @return
    */
  def resolveQuorumSetFromStatement(statement: Statement): P[F, QuorumSet]

  /**
    * is the quorum set sane?
    * @param quorumSet quorum set
    * @return
    */
  def isSane(quorumSet: QuorumSet): P[F, Boolean]
}
