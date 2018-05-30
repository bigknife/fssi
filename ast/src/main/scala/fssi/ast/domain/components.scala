package fssi.ast.domain

import bigknife.sop.effect.error.ErrorM
import bigknife.sop.macros._
import bigknife.sop.implicits._
import cats.data.Kleisli

object components {
  @sps trait Model[F[_]] {
    //// common service ////
    val log: LogService[F]
    val err: ErrorM[F]

    //// biz service ////
    val monitorService: MonitorService[F]
    val cryptoService: CryptoService[F]
    val networkService: NetworkService[F]
    val accountService: AccountService[F]
    val transactionService: TransactionService[F]

    //// biz store ////
    val accountStore: AccountStore[F]
    val transactionStore: TransactionStore[F]
  }
}