package fssi
package interpreter

import java.io.File

import fssi.ast.Store
import fssi.types.biz.{Account, Block, ChainConfiguration, Constant}
import fssi.types.exception.FSSIException
import io.circe._
import io.circe.parser._
import fssi.types.json.implicits._
import io.circe.generic.auto._
import better.files._
import fssi.base.BytesValue
import fssi.scp.interpreter.store.Var

import scala.util.Try
import fssi.store.bcs._
import fssi.store.bcs.types.BCSKey.{BlockKey, MetaKey, StateKey}
import fssi.store.bcs.types.{BlockData, MetaData}
import fssi.types.base.HashState

class StoreHandler extends Store.Handler[Stack] with LogSupport {

  val bcsVar: Var[BCS] = Var.empty

  /** create store for a chain, include data store, chain configuration store, etc. logically:
    * 1. chain.conf configuration store
    * 2. store/kv/{chainId}_block.db the block chain store
    * 3. store/kv/{chainId}_contract.db the contracts store
    * 4. store/kv/{chainId}_token.db the account token store
    */
  override def createChainStore(root: File, chainId: String): Stack[Unit] = Stack {
    // create chainId file in the root.
    import better.files._
    val f  = root.toScala
    val f1 = f.createChild(".chain")
    f1.overwrite(s"$chainId")
    f.createChild("db", asDirectory = true)
    ()
  }

  /** initialized an empty chain store, such as creating genesis block .
    */
  override def initialize(root: File, chainId: String): Stack[Unit] = Stack {
    if (bcsVar.isEmpty) {
      bcsVar := BCS(root.getAbsolutePath)
    }
    bcsVar.foreach { bcs =>
      val height: BigInt = 0
      // meta
      bcs.putMeta(height, MetaKey.ChainID, MetaData(chainId.getBytes("utf-8")))
      bcs.putMeta(height, MetaKey.Height, MetaData(height.toByteArray))
      bcs.putMeta(height, MetaKey.Version, MetaData(Constant.Chain_Version.getBytes("utf-8")))

      // block
      bcs.putBlock(BlockKey.preBlockHash(height), BlockData(Constant.Zero_Value.toByteArray))
      bcs.putBlock(BlockKey.curBlockHash(height), BlockData(Constant.Zero_Value.toByteArray))
      bcs.putBlock(BlockKey.curTransactionHash(height), BlockData(Constant.Zero_Value.toByteArray))
      bcs.putBlock(BlockKey.curReceiptHash(height), BlockData(Constant.Zero_Value.toByteArray))
      bcs.putBlock(BlockKey.curStateHash(height), BlockData(Constant.Zero_Value.toByteArray))

      // transaction None
      // receipt None
      // state None

      bcs.commit(height) match {
        case Left(t)  => throw t
        case Right(_) => ()
      }
    }
  }

  /** load from an exist store
    */
  override def load(root: File): Stack[Unit] = Stack {
    if (bcsVar.isEmpty) {
      bcsVar := BCS(root.getAbsolutePath)
      ()
    } else ()
  }

  /** unload current resource
    */
  override def unload(): Stack[Unit] = Stack {
    bcsVar.foreach(_.close())
  }

  /** self-check, if something insane, throw exceptions
    */
  override def check(): Stack[Unit] = Stack {
    // check the current block, to see:
    // 1. if the block exists
    // 2. if the blocks' states are consistent with respected mpt
    require(bcsVar.isDefined)
    bcsVar.foreach { bcs =>
      def checkHeight(height: BigInt): Unit = {
        val blockPreHash     = bcs.getPersistedBlock(BlockKey.preBlockHash(height)).right.get.get
        val blockCurrentHash = bcs.getPersistedBlock(BlockKey.curBlockHash(height)).right.get.get
        val stateHash        = bcs.getPersistedBlock(BlockKey.curStateHash(height)).right.get.get
        val transactionHash  = bcs.getPersistedBlock(BlockKey.curTransactionHash(height)).right.get.get
        val receiptHash      = bcs.getPersistedBlock(BlockKey.curReceiptHash(height)).right.get.get

        if (height == 0) {
          log.debug(s"check height @ $height")

          val zero = BlockData(Constant.Zero_Value.toByteArray)
          require(blockCurrentHash === zero, "genesis block current hash should be zero")
          require(blockPreHash === zero, "genesis block pre hash should be zero")
          require(stateHash === zero, "genesis block state hash should be zero")
          require(transactionHash === zero, "genesis block transaction hash should be zero")
          require(receiptHash === zero, "genesis block receipt hash should be zero")
        } else if (height > 0) {
          log.debug(s"check height @ $height")
          // current state
          val curBlockStateHash = bcs.getPersistedBlock(BlockKey.curStateHash(height)).right.get.get
          val curStateRootHash       = bcs.persistedStateRootHash.get
          require(curBlockStateHash.bytes sameElements curStateRootHash.bytes,
            "current state hash should be consistent")

          // previous state
          val preBlockStateHash = bcs.getPersistedBlock(BlockKey.preStateHash(height)).right.get.get
          val preStateRootHash = bcs.getPersistedBlock(BlockKey.curStateHash(height - 1)).right.get.get
          require(preBlockStateHash.bytes sameElements preStateRootHash.bytes,
            "previous state hash should be consistent")

          // pre block hash
          val curBlockHash_pre = bcs.getPersistedBlock(BlockKey.curBlockHash(height-1)).right.get.get
          val preBlockHash = bcs.getPersistedBlock(BlockKey.preBlockHash(height)).right.get.get
          require(curBlockHash_pre.bytes sameElements preBlockHash.bytes,
            "previous block hash should be consistent")

          // pre receipt hash
          // current receipt hash

          // pre transaction hash
          // current transaction hash

          val blockReceiptHash  = bcs.getPersistedBlock(BlockKey.curReceiptHash(height)).right.get.get

          val receiptRootHash     = bcs.persistedReceiptRootHash(height).get
          require(blockReceiptHash.bytes sameElements receiptRootHash.bytes,
            "receipt hash should be consistent")

          val blockPreviousHash = bcs.getPersistedBlock(BlockKey.preBlockHash(height)).right.get.get

          val blockTransactionHash =
            bcs.getPersistedBlock(BlockKey.curTransactionHash(height)).right.get.get
          val transactionRootHash = bcs.persistedTransactionRootHash(height).get

          val previousBlockHash   = bcs.persistedBlockRootHash(height - 1).get


          require(blockTransactionHash.bytes sameElements transactionRootHash.bytes,
                  "transaction hash should be consistent")

          require(blockPreviousHash.bytes sameElements previousBlockHash.bytes,
                  "previous block hash should be consistent")

          // timestamp of current height should gt the previous'
          val currentBlockTimestamp = BigInt(
            1,
            bcs.getPersistedBlock(BlockKey.blockTimestamp(height)).right.get.get.bytes).toLong
          val previousBlockTimestamp = BigInt(
            1,
            bcs.getPersistedBlock(BlockKey.blockTimestamp(height - 1)).right.get.get.bytes).toLong

          require(currentBlockTimestamp > previousBlockTimestamp)

          checkHeight(height - 1)
        } else throw new RuntimeException("height should >= 0")
      }

      val height = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      checkHeight(height)
    }

  }

  /** get conf store
    */
  override def getChainConfiguration(): Stack[ChainConfiguration] = Stack {
    new ChainConfiguration {}
  }

  override def getLatestDeterminedBlock(): Stack[Block] = {
    // get persisted and de-serialized to a block
    require(bcsVar.isDefined)
    bcsVar.map { bcs =>
      // get current height
      val height = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      if (height < 0) throw new RuntimeException(s"insane block chain store. height is $height")
      else {
        // genesis block
        // get current state root hash
        bcs.getPersistedBlock(BlockKey.st)
        // get previous block state root hash
        // get
      }

    }
  }

  override def persistBlock(block: Block): Stack[Unit] = ???

  override def loadAccountFromFile(accountFile: File): Stack[Either[FSSIException, Account]] =
    Stack {
      Try {
        val accountJsonString = accountFile.toScala.contentAsString
        val result = for {
          json    <- parse(accountJsonString)
          account <- json.as[Account]
        } yield account
        result.right.get
      }.toEither.left.map(e =>
        new FSSIException(s"load account from file ($accountFile) failed", Option(e)))
    }

  override def loadSecretKeyFromFile(
      secretKeyFile: File): Stack[Either[FSSIException, Account.SecretKey]] = Stack {
    Try {
      val secretKeyString = secretKeyFile.toScala.contentAsString
      val secretBytes     = BytesValue.unsafeDecodeBcBase58(secretKeyString).bytes
      Account.SecretKey(secretBytes)
    }.toEither.left.map(e =>
      new FSSIException(s"load secret key from file $secretKeyFile failed", Option(e)))
  }
}

object StoreHandler {
  val instance = new StoreHandler

  trait Implicits {
    implicit val storeHandler: StoreHandler = instance
  }
}
