package fssi.ast
package uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._
import java.io._

trait NewBlockGeneratedProgram[F[_]] extends CoreNodeProgram[F] with BaseProgram[F] {
  import model._

  def newBlockGenerated(block: Block): SP[F, Unit] = {
    for {
      hashVerify   <- crypto.verifyBlockHash(block)
      _            <- requireM(hashVerify(), new RuntimeException("block hash tampered"))
      currentBlock <- store.getLatestDeterminedBlock()
      _ <- requireM(currentBlock.height + 1 == block.height,
                    new RuntimeException("block height not consistent"))
      _ <- requireM(currentBlock.chainId == block.chainId,
                    new RuntimeException("chainId not consistent"))
      _ <- store.persistBlock(block)
    } yield ()
  }
}
