package fssi.ast

import bigknife.sop.effect.error.ErrorM
import bigknife.sop.macros._
import bigknife.sop.implicits._

trait BlockChain {
  @sps trait Model[F[_]] {
    val err: ErrorM[F]
    val log: Log[F]

    val network: Network[F]
    val store: Store[F]
    val consensus: Consensus[F]
    val crypto: Crypto[F]
    val contract: Contract[F]
  }
}
