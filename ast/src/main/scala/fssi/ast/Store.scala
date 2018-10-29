package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

@sp trait Store[F[_]] {
  def placeholder():P[F, Unit]
}
