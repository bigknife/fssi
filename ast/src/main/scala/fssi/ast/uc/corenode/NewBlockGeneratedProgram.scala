package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._
import fssi.types.base._
import fssi.types.biz._
import java.io._

import cats.implicits._
import fssi.types.ReceiptSet

trait NewBlockGeneratedProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def newBlockGenerated(block: Block): SP[F, Unit] = {
    for {
      _            <- consensus.stopConsensus()
      _            <- log.debug(s"try to externalize block, ${block.height} ----> ${block.hash}")
      hashVerify   <- crypto.verifyBlockHash(block)
      _            <- requireM(hashVerify(), new RuntimeException("block hash tampered"))
      currentBlock <- store.getLatestDeterminedBlock()
      _ <- requireM(currentBlock.height + 1 == block.height,
                    new RuntimeException("block height not consistent"))
      _ <- requireM(currentBlock.chainId == block.chainId,
                    new RuntimeException("chainId not consistent"))
      receipts <- block.transactions.foldLeft(ReceiptSet.empty.pureSP[F]) { (acc, n) =>
        for {
          receiptSet <- acc
          receipt    <- runTransaction(n)
        } yield receiptSet + receipt
      }
      blockToPersist <- store.blockToPersist(block, receipts)
      _              <- store.persistBlock(blockToPersist)
      _              <- log.info(s"externalized: ${block.height} ----> ${block.hash}")
      _              <- consensus.notifySubscriberWhenExternalized(block)
      _              <- attemptToAgreeTransaction()
    } yield ()
  }
}
