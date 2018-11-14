package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait HandleTransactionProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def handleTransaction(transaction: Transaction): SP[F, Receipt] = {
    for {
      verifyResult      <- crypto.verifyTransactionSignature(transaction)
      _                 <- requireM(verifyResult(), new RuntimeException("transaction signature tampered"))
      receipt           <- contract.runTransaction(transaction)
      determinedBlock   <- store.getLatestDeterminedBlock()
      currentWorldState <- store.getCurrentWorldState()
      _                 <- consensus.tryAgree(transaction, receipt, determinedBlock, currentWorldState)
    } yield receipt
  }
}
