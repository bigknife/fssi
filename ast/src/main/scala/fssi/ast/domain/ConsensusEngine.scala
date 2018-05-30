package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types._

@sp trait ConsensusEngine[F[_]] {
  def poolMoment(moment: Moment): P[F, Unit]
}
