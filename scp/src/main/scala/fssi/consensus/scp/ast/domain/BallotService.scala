package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.consensus.scp.ast.domain.types._

@sp trait BallotService[F[_]] {

  def areBallotsLessAndIncompatible(a1: Ballot, a2: Ballot): P[F, Boolean]

  def areBallotsLessAndCompatible(a1: Ballot, a2: Ballot): P[F, Boolean]

  def areBallotsCompatible(a1: Ballot, a2: Ballot): P[F, Boolean]
}
