package fssi
package ast
package uc
package tool

import fssi.types.biz._
import fssi.types.base._

import bigknife.sop._
import bigknife.sop.implicits._
import types.implicits._

import java.io._

trait LocalChainProgram[F[_]] extends BaseProgram[F] {
  import model._

  /** Create a chain
    * @param dataDir directory where the chain data saved
    * @param chainId the chain id
    */
  def createChain(rootDir: File, chainId: String): SP[F, Unit] = {
    import chainStore._
    import blockStore._
    import tokenStore._
    import contractStore._
    import contractDataStore._
    import dataStore._
    import receiptStore._
    import blockService._
    import log._

    for {
      root              <- createChainSkeleton(rootDir, chainId).right
      confFile          <- createDefaultConfigFile(root)
      blockStoreRoot    <- getBlockStoreRoot(root)
      _                 <- initializeBlockStore(blockStoreRoot)
      tokenStoreRoot    <- getTokenStoreRoot(root)
      _                 <- initializeTokenStore(tokenStoreRoot)
      contractStoreRoot <- getContractStoreRoot(root)
      _                 <- initializeContractStore(contractStoreRoot)
      dataStoreRoot     <- getDataStoreRoot(root)
      _                 <- initializeDataStore(dataStoreRoot)
      receiptStoreRoot  <- getReceiptStoreRoot(root)
      _                 <- initializeReceiptStore(receiptStoreRoot)
      block             <- createFirstBlock(chainId)
      hashedBlock       <- hashBlock(block)
      _                 <- saveBizBlock(hashedBlock)
      _                 <- info(s"chain initialized, please edit the default config file: $confFile")
    } yield ()
  }

}
