package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.base._
import fssi.types.biz._

import fssi.types.biz.Transaction._

trait HandleTransactionProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def handleTransaction(transaction: Transaction): SP[F, Receipt] = {
    for {
      verifyResult      <- crypto.verifyTransactionSignature(transaction)
      _                 <- requireM(verifyResult(), new RuntimeException("transaction signature tampered"))
      receipt           <- runTransaction(transaction)
      determinedBlock   <- store.getLatestDeterminedBlock()
      currentWorldState <- store.getCurrentWorldState()
      _                 <- consensus.tryAgree(transaction, receipt, determinedBlock, currentWorldState)
    } yield receipt
  }

  private def runTransaction(transaction: Transaction): SP[F, Receipt] = transaction match {
    case transfer: Transfer => runTransferTransaction(transfer)
    case deploy: Deploy     => runDeployTransaction(deploy)
    case run: Run           => runRunTransaction(run)
  }

  private def runTransferTransaction(transfer: Transfer): SP[F, Receipt] = ???

  private def runDeployTransaction(deploy: Deploy): SP[F, Receipt] = ???

  private def runRunTransaction(run: Run): SP[F, Receipt] = ???
}
