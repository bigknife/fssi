package fssi
package ast
package uc

import utils._
import types._, exception._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._
import scala.collection._

import java.io.File

trait CoreNodeProgram[F[_]] extends BaseProgram[F] with CoreNodeProgramHelper[F] {
  import model._

  /** Start up a core node.
    * @return node info
    */
  def startup(dataDir: File, handler: JsonMessageHandler): SP[F, Node] = {
    import network._
    import consensusEngine._
    import tokenStore._
    import contractStore._
    import contractDataStore._
    import blockStore._
    import log._

    for {
      _                       <- info(s"starting core node at $dataDir")
      _                       <- initializeBlockStore(dataDir)
      _                       <- info("  initialized block store")
      _                       <- initializeTokenStore(dataDir)
      _                       <- info("  initialized token store")
      _                       <- initializeContractStore(dataDir)
      _                       <- info("  initialized contract stores")
      _                       <- initializeContractDataStore(dataDir)
      _                       <- info("  initialized contract data store")
      block                   <- getLatestDeterminedBlock()
      _                       <- info(s"  found current block height: ${block.height}, hash: ${block.hash}")
      tokenStoreIsSane        <- testTokenStore(block)
      _                       <- info(s"  token store is sane? $tokenStoreIsSane")
      contractStoreIsSane     <- testContractStore(block)
      _                       <- info(s"  contract store is sane? $contractStoreIsSane")
      contractDataStoreIsSane <- testContractDataStore(block)
      _                       <- info(s"  contract data store is sane? $contractDataStoreIsSane")
      _ <- requireM(tokenStoreIsSane && contractStoreIsSane && contractDataStoreIsSane,
                    new FSSIException("local stores are not sane"))
      n1 <- startupP2PNode(handler)
      n2 <- bindAccount(n1)
      _  <- setCurrentNode(n2)
      _  <- initializeConsensusEngine(n2.account.get)
      _  <- info("initialized consensus engine")
      _  <- info(s"node startup and binding account: $n2")
    } yield n2
  }

  /** Shutdown core node
    */
  def shutdown(node: Node): SP[F, Unit] =
    for {
      _ <- network.shutdownP2PNode(node)
    } yield ()

  /** handle transaction
    */
  def handleTransaction(transaction: Transaction): SP[F, Unit] = {
    import transactionService._
    import blockStore._
    import crypto._
    import consensusEngine._
    import network._
    for {
      toBeSingedBytes <- calculateSingedBytesOfTransaction(transaction)
      verified <- verifySignature(toBeSingedBytes,
                                  transaction.publicKeyForVerifying,
                                  transaction.signature)
      _ <- log.info(s"transaction(${transaction.id}) verified: $verified")

      _ <- requireM(
        verified,
        new FSSIException(s"Transaction of id=${transaction.id} signature can't be verified"))
      determinedBlock    <- getLatestDeterminedBlock()
      undeterminedBlock <- appendTransactionToUnDeterminedBlock(determinedBlock, transaction)
      node               <- network.getCurrentNode()
      _ <- log.info("try to agree block...")
      _                  <- tryToAgreeBlock(node.account.get, undeterminedBlock, undeterminedBlock)
    } yield ()
  }

  /** handle something when a block is determined
    * @param account current node bound account
    * @param targetHeight target height, the index number that the agreement want's to store value in
    * @param block the final value reached agreement
    */
  def handleBlockReachedAgreement(account: Account.ID,
                                  targetHeight: BigInt,
                                  block: Block): SP[F, Unit] = {
    // 1. check the block height, current latest height and the targetHeight is consistent
    //    run every transaction in this block
    //    if no error, save block to local store
    // 2. check block's chainID

    import blockStore._
    import blockService._
    import log._

    def tempRunAllTransactions(
        transactions: immutable.TreeSet[Transaction]): SP[F, Either[Throwable, Unit]] = {
      import Transaction._

      val init   = (Right(()): Either[Throwable, Unit]).pureSP[F]
      val height = block.height

      transactions.foldLeft(init) { (acc, n) =>
        for {
          last <- acc
          r <- last match {
            case x @ Left(_) => (x: Either[Throwable, Unit]).pureSP[F]
            case Right(_) =>
              n match {
                case x: Transfer        => tempRunTransfer(height, x)
                case x: PublishContract => tempRunPublishContract(height, x)
                case x: RunContract     => tempRunRunContract(height, x)
              }
          }
        } yield r
      }
    }

    for {
      hashOk       <- verifyBlockHash(block)
      _            <- requireM(hashOk, new FSSIException("block's hash verified failed"))
      currentBlock <- getLatestDeterminedBlock()
      _ <- requireM(
        currentBlock.chainID == block.chainID,
        new FSSIException(
          s"inconsistent chainID: local = ${currentBlock.chainID}, block = ${block.chainID}"))
      _ <- requireM(
        targetHeight == currentBlock.height + 1 && block.height == targetHeight,
        new FSSIException(
          s"inconsistent block height: local = ${currentBlock.height}, target = $targetHeight, block = ${block.height}")
      )
      _ <- requireM(
        block.previousHash == currentBlock.hash,
        new FSSIException(
          s"inconsistent hash, block's preivous = ${block.previousHash}, current's hash = ${currentBlock.hash}")
      )
      // run every transaction
      result <- tempRunAllTransactions(block.transactions)
      _ <- result match {
        case Left(t) =>
          for {
            _ <- info(s"run block(height=${block.height}) transactions failed", Some(t))
            _ <- rollback(block.height)
          } yield ()
        case Right(_) =>
          for {
            _ <- info(s"run block(height=${block.height}) transactions successfully")
            _ <- commit(block.height)
          } yield ()
      }
    } yield ()
  }

  /** get current core node height
    */
  def currentHeight(): SP[F, BigInt] = blockStore.getLatestDeterminedBlock().map(_.height)

  /** handle consensus aux message
    */
  def handleConsensusAuxMessage(auxMessage: ConsensusAuxMessage): SP[F, Unit] =
    for {
      node <- network.getCurrentNode()
      _    <- consensusEngine.handleConsensusAuxMessage(node.account.get, auxMessage)
    } yield ()

}
object CoreNodeProgram {
  def apply[F[_]](implicit M: components.Model[F]): CoreNodeProgram[F] = new CoreNodeProgram[F] {
    val model: components.Model[F] = M
  }
}
