package fssi
package ast

import bigknife.sop.effect.error.ErrorM
import bigknife.sop.macros._
import bigknife.sop.implicits._
import cats.data.Kleisli

object components {
  @sps trait Model[F[_]] {

    val err: ErrorM[F]
    val log: LogService[F]

    val crypto: Crypto[F]
    val network: Network[F]
    val blockService: BlockService[F]
    val chainStore: ChainStore[F]
    val blockStore: BlockStore[F]
    val tokenStore: TokenStore[F]
    val contractStore: ContractStore[F]
    val contractDataStore: ContractDataStore[F]
    val transactionService: TransactionService[F]
    val consensusEngine: ConsensusEngine[F]
    val accountStore: AccountStore[F]
    val contractService: ContractService[F]
  }
}
