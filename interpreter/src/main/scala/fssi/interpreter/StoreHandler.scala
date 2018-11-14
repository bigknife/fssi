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
import fssi.store.bcs.types._
import fssi.store.bcs.types.BCSKey._
import fssi.types.base._
import fssi.types._
import fssi.types.biz._
import fssi.types.implicits._

class StoreHandler extends Store.Handler[Stack] with LogSupport {

  val bcsVar: Var[BCS] = Var.empty

  /** create store for a chain, include data store, chain configuration store, etc.
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
  override def initialize(root: File, chainId: String): Stack[Unit] = {
    if (bcsVar.isEmpty) {
      bcsVar := BCS(root.getAbsolutePath)
    }

    // genesis block
    val block: Block = Block.genesis(chainId)

    for {
      _ <- persistBlock(block)
    } yield ()
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
        if (height > 0) {
          val blockHeight =
            BigInt(1, bcs.getPersistedBlock(BlockKey.blockHeight(height)).right.get.get.bytes)
          require(blockHeight == height, "block height is not consistent")

          val metaChainId  = bcs.getPersistedMeta(MetaKey.ChainID).right.get.get
          val blockChainId = bcs.getPersistedBlock(BlockKey.blockChainId(height)).right.get.get
          require(metaChainId.bytes sameElements blockChainId.bytes, "chain id is not consistent")

          val preWorldState = bcs.getPersistedBlock(BlockKey.preWorldState(height)).right.get.get
          val preBlockWorldState =
            bcs.getPersistedBlock(BlockKey.curWorldState(height - 1)).right.get.get
          require(preWorldState === preBlockWorldState, "preWorldState is not consistent")

          val timestamp =
            BigInt(1, bcs.getPersistedBlock(BlockKey.blockTimestamp(height)).right.get.get.bytes)
          val preTimestamp =
            BigInt(1,
                   bcs.getPersistedBlock(BlockKey.blockTimestamp(height - 1)).right.get.get.bytes)
          require(preTimestamp > timestamp, "timestamp is not sane")

          checkHeight(height - 1)
        } else ()
      }

      val currentHeight = BigInt(1, bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      if (currentHeight > 0) checkHeight(currentHeight)
      else ()
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
      val height  = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      val chainId = new String(bcs.getPersistedMeta(MetaKey.ChainID).right.get.get.bytes, "utf-8")
      if (height < 0) throw new RuntimeException(s"insane block chain store. height is $height")
      else if (height == 0) Block.genesis(chainId)
      else {
        // genesis block
        val preWorldState =
          WorldState(bcs.getPersistedBlock(BlockKey.preWorldState(height)).right.get.get.bytes)
        val curWorldState =
          WorldState(bcs.getPersistedBlock(BlockKey.curWorldState(height)).right.get.get.bytes)
        val timestamp =
          Timestamp(BigInt(
            1,
            bcs.getPersistedBlock(BlockKey.blockTimestamp(height)).right.get.get.bytes).toLong)
        val hash =
          Hash(bcs.getPersistedBlock(BlockKey.blockHash(height)).right.get.get.bytes)

        val transactionIds: Array[String] = { // bcBase58
          val s =
            new String(bcs.getPersistedBlock(BlockKey.transactionList(height)).right.get.get.bytes)
          s.split("\n").filter(_.length > 0)
        }

        val transactions: TransactionSet = TransactionSet(transactionIds.map { tid =>
          // tid is base58 of transaction id
          deserializeTransaction(
            bcs.getPersistedTransaction(TransactionKey.transaction(height, tid)).right.get.get)
        }: _*)

        val receipts: ReceiptSet = ReceiptSet(transactionIds.map { tid =>
          // tid is base58 of transaction id
          val result = 1 == BigInt(
            1,
            bcs.getPersistedReceipt(ReceiptKey.receiptResult(height, tid)).right.get.get.bytes)
          val costs = BigInt(
            1,
            bcs.getPersistedReceipt(ReceiptKey.receiptCost(height, tid)).right.get.get.bytes).toInt
          val logs = deserializeReceiptLogs(
            bcs.getPersistedReceipt(ReceiptKey.receiptLogs(height, tid)).right.get.get)
          Receipt(
            transactionId = Transaction.ID(BytesValue.decodeBcBase58(tid).get.bytes),
            success = result,
            logs = logs,
            costs = costs
          )
        }: _*)

        Block(
          height = height,
          chainId = chainId,
          preWorldState = preWorldState,
          curWorldState = curWorldState,
          transactions = transactions,
          receipts = receipts,
          timestamp = timestamp,
          hash = hash
        )
      }

    }

    ???
  }

  override def getCurrentWorldState(): Stack[WorldState] = ???

  override def persistBlock(block: Block): Stack[Unit] = Stack {
    // 1. meta height
    // 2. block blala...
    // 3. transcation and receipt
    require(bcsVar.isDefined)
    bcsVar.foreach { bcs =>
      val height = block.height
      //block
      bcs.putMeta(height, MetaKey.Height, MetaData(block.height.toByteArray))
      bcs.putBlock(BlockKey.preWorldState(height), BlockData(block.preWorldState.value))
      bcs.putBlock(BlockKey.curWorldState(height), BlockData(block.curWorldState.value))
      bcs.putBlock(BlockKey.transactionList(height),
                   BlockData(
                     block.transactions
                       .map(_.id.value.asBytesValue.bcBase58)
                       .mkString("\n")
                       .getBytes("utf-8")))
      bcs.putBlock(BlockKey.blockTimestamp(height), BlockData(block.timestamp.asBytesValue.bytes))
      bcs.putBlock(BlockKey.blockHash(height), BlockData(block.hash.asBytesValue.bytes))

      //transaction
      block.transactions.foreach { transaction =>
        val transactionId = transaction.id.value.asBytesValue.bcBase58
        bcs.putTransaction(TransactionKey.transaction(height, transactionId),
                           serializeTransaction(transaction))
        // receipt
        val receipt = block.receipts.find(_.transactionId === transaction.id)
        receipt.foreach { r =>
          val result = if (r.success) BigInt(1).toByteArray else BigInt(0).toByteArray
          val cost   = BigInt(r.costs).toByteArray
          bcs.putReceipt(ReceiptKey.receiptResult(height, transactionId), ReceiptData(result))
          bcs.putReceipt(ReceiptKey.receiptCost(height, transactionId), ReceiptData(cost))
          bcs.putReceipt(ReceiptKey.receiptLogs(height, transactionId),
                         serializeReceiptLogs(r.logs))
        }

      //todo: if the transaction is deploy, then persist the contract

      }

      bcs.commit(height)
      ()
    }
  }

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

  //private def persistTransaction(bcs: BCS, transaction: Transaction): Unit = ???

  // transaction protocol:
  // first line used to express types
  // following lines used to express every fields
  private def serializeTransaction(t: Transaction): TransactionData = t match {
    case x: Transaction.Transfer =>
      TransactionData(
        Vector(
          "transfer",
          x.id.asBytesValue.bcBase58,
          x.payer.asBytesValue.bcBase58,
          x.payee.asBytesValue.bcBase58,
          x.token.asBytesValue.bcBase58,
          x.signature.asBytesValue.bcBase58,
          x.timestamp.asBytesValue.bcBase58
        ).mkString("\n").getBytes("utf-8"))
    case x: Transaction.Deploy =>
      TransactionData(
        Vector(
          "deploy",
          x.id.asBytesValue.bcBase58,
          x.owner.asBytesValue.bcBase58,
          x.contract.asBytesValue.bcBase58,
          x.signature.asBytesValue.bcBase58,
          x.timestamp.asBytesValue.bcBase58
        ).mkString("\n").getBytes("utf-8"))
    case x: Transaction.Run =>
      TransactionData(
        Vector(
          "run",
          x.id.asBytesValue.bcBase58,
          x.caller.asBytesValue.bcBase58,
          x.contractName.asBytesValue.bcBase58,
          x.contractVersion.asBytesValue.bcBase58,
          x.methodAlias.asBytesValue.bcBase58,
          x.contractParameter.asBytesValue.bcBase58,
          x.signature.asBytesValue.bcBase58,
          x.timestamp.asBytesValue.bcBase58
        ).mkString("\n").getBytes("utf-8"))

    case _ => TransactionData(Array.emptyByteArray)
  }
  private def deserializeTransaction(b: TransactionData): Transaction = {
    ???
  }

  private def serializeReceiptLogs(t: Vector[Receipt.Log]): ReceiptData   = ???
  private def deserializeReceiptLogs(b: ReceiptData): Vector[Receipt.Log] = ???
}

object StoreHandler {
  val instance = new StoreHandler

  trait Implicits {
    implicit val storeHandler: StoreHandler = instance
  }
}
