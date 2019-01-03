package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.base._
import fssi.types.biz._

trait HandleTransactionProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def handleTransaction(transaction: Transaction): SP[F, Unit] = {
    for {
      _ <- store.acceptNewTransaction(transaction)
      _ <- ifThen(consensus.isConsensusLasting().map(!_))(
        consensus.prepareExecuteAgreeProgram(attemptToAgreeTransaction()))
    } yield ()
  }

  override def attemptToAgreeTransaction(): SP[F, Unit] = {

    def canAgreeTransaction(): SP[F, Boolean] =
      ifM(consensus.isConsensusLasting(), false.pureSP[F])(store.hasPreparedTransactions())

    for {
      _ <- ifThen(canAgreeTransaction()) {
        for {
          _            <- log.debug("--- attempt to agree transaction ---")
          _            <- consensus.startConsensus()
          _            <- store.calculateTransactions()
          transactions <- store.transactionsToAgree()
          _            <- log.info(s" try to agree transactions: ${transactions.size}")
          _            <- store.clearTransactions(transactions)
          _            <- consensus.agreeTransactions(transactions)
        } yield ()
      }
    } yield ()
  }

}
