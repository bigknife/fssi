package fssi.scp
package ast

import bigknife.sop.effect.error.ErrorM
import bigknife.sop.macros._
import bigknife.sop.implicits._

object components {
  @sps trait Model[F[_]] {
    val err: ErrorM[F]

    val nodeService: NodeService[F]
    val nodeStore: NodeStore[F]
    val applicationService: ApplicationService[F]
  }
}

