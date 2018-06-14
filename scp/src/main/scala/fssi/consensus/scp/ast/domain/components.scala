package fssi.consensus.scp.ast.domain

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

object components {
  @sps trait Model[F[_]] {
    val envelopeService: EnvelopeService[F]
    val slotService: SlotService[F]
    val statementService: StatementService[F]
    val quorumSetService: QuorumSetService[F]
    val ballotService: BallotService[F]

    val slotStore: SlotStore[F]
    val statementStore: StatementStore[F]
    val envelopeStore: EnvelopeStore[F]
  }
}
