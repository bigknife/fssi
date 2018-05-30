package fssi.ast.domain

import bigknife.sop._, macros._, implicits._

@sp trait MonitorService[F[_]] {
  def startNow(): P[F, Long]
  def timePassed(start: Long): P[F, Long]
}
