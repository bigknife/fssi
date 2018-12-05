package fssi
package interpreter

import java.io.File

import fssi.ast.Store
import fssi.types.biz.{Account, Block}
import fssi.types.exception.FSSIException
import io.circe._
import io.circe.parser._
import fssi.types.json.implicits._
import io.circe.syntax._
import io.circe.generic.auto._
import better.files._
import fssi.base.BytesValue
import fssi.contract.lib.{Context, KVStore, TokenQuery}
import fssi.scp.interpreter.store.Var

import scala.util.Try
import fssi.store.bcs._
import fssi.store.bcs.types._
import fssi.store.bcs.types.BCSKey._
import fssi.types.base._
import fssi.types._
import fssi.types.biz.Contract.Version
import fssi.types.biz.Contract.UserContract
import fssi.types.biz.Transaction.{Deploy, Run, Transfer}
import fssi.types.biz._
import fssi.types.implicits._
import fssi.utils._

class StoreHandler extends Store.Handler[Stack] with LogSupport with UnsignedBytesSupport {

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
    val confString =
      Resource.getAsString("config-sample.conf")(java.nio.charset.Charset.forName("utf-8"))
    val confFile = f.createChild("fssi.conf", asDirectory = false, createParents = true)
    confFile.overwrite(confString)
    ()
  }

  /** initialized an empty chain store, such as creating genesis block .
    */
  override def initialize(root: File, chainId: String): Stack[Unit] = {
    if (bcsVar.isEmpty) {
      bcsVar := BCS(s"${root.getAbsolutePath}/db")
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
      bcsVar := BCS(s"${root.getAbsolutePath}/db")
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
          require(metaChainId.bytes sameElements blockChainId.bytes, "in id is not consistent")

          val preWorldState = bcs.getPersistedBlock(BlockKey.preWorldState(height)).right.get.get
          val preBlockWorldState =
            bcs.getPersistedBlock(BlockKey.curWorldState(height - 1)).right.get.get
          require(preWorldState === preBlockWorldState, "preWorldState is not consistent")

          val timestamp =
            BigInt(1, bcs.getPersistedBlock(BlockKey.blockTimestamp(height)).right.get.get.bytes)
          val preTimestamp =
            BigInt(1,
                   bcs.getPersistedBlock(BlockKey.blockTimestamp(height - 1)).right.get.get.bytes)
          require(preTimestamp < timestamp, "timestamp is not sane")

          checkHeight(height - 1)
        } else ()
      }

      val currentHeight = BigInt(1, bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      if (currentHeight > 0) checkHeight(currentHeight)
      else ()
    }
  }

  override def getLatestDeterminedBlock(): Stack[Block] = Stack {
    // get persisted and de-serialized to a block
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
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
              new String(
                bcs.getPersistedBlock(BlockKey.transactionList(height)).right.get.get.bytes)
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
            val costs = BigInt(1,
                               bcs
                                 .getPersistedReceipt(ReceiptKey.receiptCost(height, tid))
                                 .right
                                 .get
                                 .get
                                 .bytes).toInt
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
      .unsafe()
  }

  override def getPreviousBlockWorldState(): Stack[WorldState] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        val height = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
        bcs.getPersistedBlock(BlockKey.curWorldState(height)) match {
          case Right(data) =>
            data match {
              case Some(value) => WorldState(value.bytes)
              case None =>
                throw new FSSIException(s"not found current state in block height $height")
            }
        }
      }
      .unsafe()
  }

  override def persistBlock(block: Block): Stack[Unit] = Stack {
    // 1. meta height
    // 2. block blala...
    // 3. transcation and receipt
    require(bcsVar.isDefined)
    bcsVar.foreach { bcs =>
      val height = block.height
      //block
      bcs.putMeta(height, MetaKey.ChainID, MetaData(block.chainId.getBytes("utf-8")))
      bcs.putMeta(height, MetaKey.Height, MetaData(block.height.toByteArray))
      //todo: remove magic string
      bcs.putMeta(height, MetaKey.Version, MetaData("0.1".getBytes("utf-8")))

      bcs.putBlock(BlockKey.blockHeight(height), BlockData(height.toByteArray))
      bcs.putBlock(BlockKey.blockChainId(height), BlockData(block.chainId.getBytes("utf-8")))

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
      }

      bcs.commit(height)
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

  override def canDeployNewTransaction(deploy: Transaction.Deploy): Stack[Boolean] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        val accountId    = deploy.owner.asBytesValue.bcBase58
        val contractName = deploy.contract.name.asBytesValue.bcBase58
        bcs.getPersistedState(StateKey.contractVersion(accountId, contractName)).right.get match {
          case Some(data) =>
            val str = data.bytes.asBytesValue.utf8String
            Version(str) match {
              case Some(value) => deploy.contract.version > value
              case None        => throw new FSSIException(s"user contract version $str in invalid")
            }
          case None => true
        }
      }
      .unsafe()
  }

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
          x.publicKeyForVerifying.asBytesValue.bcBase58,
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
          x.publicKeyForVerifying.asBytesValue.bcBase58,
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
          x.publicKeyForVerifying.asBytesValue.bcBase58,
          x.owner.asBytesValue.bcBase58,
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
    val s = new String(b.bytes)
    s.split("\n") match {
      case Array("transfer", id, payer, publicKey, payee, token, signature, timestamp) =>
        Transaction.Transfer(
          id = Transaction.ID(BytesValue.decodeBcBase58(id).get.bytes),
          payer = Account.ID(BytesValue.decodeBcBase58(payer).get.bytes),
          publicKeyForVerifying = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes),
          payee = Account.ID(BytesValue.decodeBcBase58(payee).get.bytes),
          token = Token.parse(new String(BytesValue.decodeBcBase58(token).get.bytes)),
          signature = Signature(BytesValue.decodeBcBase58(signature).get.bytes),
          timestamp = BigInt(1, BytesValue.decodeBcBase58(timestamp).get.bytes).toLong
        )
      case Array("deploy", id, owner, publicKey, contract, signature, timestamp) =>
        Transaction.Deploy(
          id = Transaction.ID(BytesValue.decodeBcBase58(id).get.bytes),
          owner = Account.ID(BytesValue.decodeBcBase58(owner).get.bytes),
          publicKeyForVerifying = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes),
          contract = Contract.UserContract.fromDeterminedBytes(
            BytesValue.decodeBcBase58(contract).get.bytes),
          signature = Signature(BytesValue.decodeBcBase58(signature).get.bytes),
          timestamp = BigInt(1, BytesValue.decodeBcBase58(timestamp).get.bytes).toLong
        )
      case Array("run",
                 id,
                 caller,
                 publicKey,
                 owner,
                 contractName,
                 contractVersion,
                 methodAlias,
                 contractParameter,
                 signature,
                 timestamp) =>
        Transaction.Run(
          id = Transaction.ID(BytesValue.decodeBcBase58(id).get.bytes),
          caller = Account.ID(BytesValue.decodeBcBase58(caller).get.bytes),
          publicKeyForVerifying = Account.PubKey(BytesValue.decodeBcBase58(publicKey).get.bytes),
          owner = Account.ID(BytesValue.decodeBcBase58(owner).get.bytes),
          contractName = UniqueName(BytesValue.decodeBcBase58(contractName).get.bytes),
          contractVersion =
            Version(new String(BytesValue.decodeBcBase58(contractVersion).get.bytes, "utf-8")).get,
          methodAlias = new String(BytesValue.decodeBcBase58(methodAlias).get.bytes, "utf-8"),
          contractParameter = Contract.UserContract.parameterFromDeterminedBytes(
            BytesValue.decodeBcBase58(contractParameter).get.bytes),
          signature = Signature(BytesValue.decodeBcBase58(signature).get.bytes),
          timestamp = BigInt(1, BytesValue.decodeBcBase58(timestamp).get.bytes).toLong
        )
      case _ => throw new RuntimeException("insane transaction data")
    }
  }

  private def serializeReceiptLogs(t: Vector[Receipt.Log]): ReceiptData =
    ReceiptData(Receipt.logsToDeterminedBytes(t))
  private def deserializeReceiptLogs(b: ReceiptData): Vector[Receipt.Log] =
    if (b.bytes.isEmpty) Vector.empty
    else Receipt.logsFromDeterminedBytes(b.bytes)

  override def snapshotTransaction(transaction: Transaction): Stack[Either[FSSIException, Unit]] =
    Stack {
      require(bcsVar.isDefined)
      bcsVar
        .map { bcs =>
          val height = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes) + 1
          val transactionIdsBytes = bcs
            .getSnapshotTransaction(TransactionKey.transactionIds(height))
            .right
            .get
            .map(_.bytes)
            .getOrElse(Array.emptyByteArray)
          val transactionIds =
            new String(transactionIdsBytes, "utf-8").split("\n").toSet.filter(_.nonEmpty)
          val newTransactionIds = transactionIds + transaction.id.asBytesValue.bcBase58
          bcs.putTransaction(TransactionKey.transactionIds(height),
                             TransactionData(newTransactionIds.mkString("\n").getBytes("utf-8")))
          transaction match {
            case transfer: Transfer =>
              bcs.snapshotTransact { proxy =>
                val payerId           = transfer.payer.asBytesValue.bcBase58
                val payeeId           = transfer.payee.asBytesValue.bcBase58
                val token             = transfer.token
                val payerCurrentToken = proxy.getBalance(payerId)
                if (payerCurrentToken - token.amount < 0)
                  throw new FSSIException(
                    s"payer $payeeId current token $payerCurrentToken is not enough to transact $token")
                else {
                  val payeeCurrentToken = proxy.getBalance(payeeId)
                  proxy.putBalance(payeeId, payeeCurrentToken + token.amount)
                  proxy.putBalance(payerId, payerCurrentToken - token.amount)
                }
              }
            case deploy: Deploy =>
              val userContract  = deploy.contract
              val contractOwner = userContract.owner.asBytesValue.bcBase58
              val contractName  = userContract.name.asBytesValue.bcBase58
              val version       = userContract.version.asBytesValue.bcBase58
              bcs.putState(height,
                           StateKey.contractOwner(contractOwner, contractName),
                           StateData(userContract.owner.value))
              val contractsBytes = bcs
                .getPersistedState(StateKey.contractNames(contractOwner))
                .right
                .get
                .map(_.bytes)
                .getOrElse(Array.emptyByteArray)
              val contractNames =
                new String(contractsBytes, "utf-8").split("\n").toSet.filter(_.nonEmpty)
              val newContractNames = contractNames + contractName
              bcs.putState(height,
                           StateKey.contractNames(contractOwner),
                           StateData(newContractNames.mkString("\n").getBytes("utf-8")))
              bcs.putState(height,
                           StateKey.contractVersion(contractOwner, contractName),
                           StateData(userContract.version.asBytesValue.bytes))
              bcs.putState(height,
                           StateKey.contractCode(contractOwner, contractName, version),
                           StateData(userContract.code.value))
              bcs.putState(
                height,
                StateKey.contractMethods(contractOwner, contractName, version),
                StateData(Contract.UserContract.methodsToDeterminedBytes(userContract.methods)))
              bcs.putState(height,
                           StateKey.contractDesc(contractOwner, contractName, version),
                           StateData(userContract.description.value))
            case run: Run =>
              bcs.putState(height,
                           StateKey.contractInvoking(run.caller.asBytesValue.bcBase58,
                                                     run.contractName.asBytesValue.bcBase58),
                           StateData(run.asBytesValue.bytes))
          }
        }
        .unsafe()
        .left
        .map(x => new FSSIException(x.getMessage, Some(x)))
    }

  override def loadContractCode(owner: Account.ID,
                                contractName: UniqueName,
                                version: Contract.Version): Stack[Option[UserContract.Code]] =
    Stack {
      require(bcsVar.isDefined)
      bcsVar
        .map { bcs =>
          bcs
            .getPersistedState(
              StateKey.contractCode(owner.asBytesValue.bcBase58,
                                    contractName.asBytesValue.bcBase58,
                                    version.asBytesValue.bcBase58))
            .right
            .get
            .map(x => UserContract.Code(x.bytes))
        }
        .unsafe()
    }

  override def currentHeight(): Stack[BigInt] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
      }
      .getOrElse(0)
  }

  override def prepareKVStore(caller: Account.ID,
                              contractName: UniqueName,
                              contractVersion: Version): Stack[KVStore] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        val height    = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes) + 1
        val accountId = caller.asBytesValue.bcBase58
        val name      = contractName.asBytesValue.bcBase58
        val version   = contractVersion.asBytesValue.bcBase58
        new KVStore {
          override def put(key: Array[Byte], value: Array[Byte]): Unit = {
            bcs.putState(
              height,
              StateKey.contractDb(accountId, name, version, key.asBytesValue.bcBase58),
              StateData(value)
            ) match {
              case Right(_) => ()
              case Left(e)  => throw e
            }
          }
          override def get(key: Array[Byte]): Array[Byte] = {
            bcs.getStateGreedily(
              StateKey.contractDb(accountId, name, version, key.asBytesValue.bcBase58)) match {
              case Right(data) =>
                data.map(_.bytes) match {
                  case Some(value) => value
                  case None =>
                    throw new FSSIException(
                      s"value of key ${key.asBytesValue.bcBase58} not found in contract $name for caller $accountId")
                }
              case Left(e) => throw e
            }
          }
        }
      }
      .unsafe()
  }

  override def prepareTokenQuery(): Stack[TokenQuery] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        new TokenQuery {
          override def getAmount(accountId: java.lang.String): java.lang.Long =
            bcs.getPersistedBalance(accountId).right.get.toLong
        }
      }
      .unsafe()
  }

  override def createContextInstance(store: KVStore,
                                     query: TokenQuery,
                                     invoker: Account.ID): Stack[Context] = Stack {
    new Context {
      override def kvStore(): KVStore         = store
      override def tokenQuery(): TokenQuery   = query
      override def currentAccountId(): String = invoker.asBytesValue.bcBase58
    }
  }

  override def isTransactionDuplicated(transaction: Transaction): Stack[Boolean] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        val height = BigInt(bcs.getPersistedMeta(MetaKey.Height).right.get.get.bytes)
        val currentBlockTransactionIdsBytes = bcs
          .getSnapshotTransaction(TransactionKey.transactionIds(height + 1))
          .right
          .get
          .map(_.bytes)
          .getOrElse(Array.emptyByteArray)
        val currentBlockTransactionIds =
          new String(currentBlockTransactionIdsBytes, "utf-8").split("\n").toSet
        val transactionId = transaction.id.asBytesValue.bcBase58
        if (currentBlockTransactionIds.contains(transactionId)) true
        else {
          val preBlockTransactionIdsBytes = bcs
            .getPersistedTransaction(TransactionKey.transactionIds(height))
            .right
            .get
            .map(_.bytes)
            .getOrElse(Array.emptyByteArray)
          val preBlockTransactionIds =
            new String(preBlockTransactionIdsBytes, "utf-8").split("\n").toSet
          preBlockTransactionIds.contains(transactionId)
        }
      }
      .unsafe()
  }

  override def blockToPersist(block: Block, receipts: ReceiptSet): Stack[Block] = Stack {
    require(bcsVar.isDefined)
    bcsVar
      .map { bcs =>
        var ultimateStateBytes = Array.emptyByteArray
        bcs.temporarilyCommit(block.height)(state => ultimateStateBytes = state.stateRootHash)
        val newBlock =
          block.copy(curWorldState = WorldState(ultimateStateBytes), receipts = receipts)
        newBlock.copy(hash = Hash(crypto.hash(calculateUnsignedBlockBytes(newBlock))))
      }
      .unsafe()
  }
}

object StoreHandler {
  val instance = new StoreHandler

  trait Implicits {
    implicit val storeHandler: StoreHandler = instance
  }
}
