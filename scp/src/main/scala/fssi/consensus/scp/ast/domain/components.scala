package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

object components {
  @sps trait Model[F[_]] {
    val envelopeService: EnvelopeService[F]
    val slotService: SlotService[F]

    val slotStore: SlotStore[F]
  }
}
