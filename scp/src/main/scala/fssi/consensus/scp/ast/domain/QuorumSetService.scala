package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

trait QuorumSetService[F[_]] {
  def isVBlocking()
}
