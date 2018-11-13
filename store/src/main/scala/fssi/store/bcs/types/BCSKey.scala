package fssi.store.bcs.types

import java.net.URLEncoder

import org.slf4j.LoggerFactory

sealed trait BCSKey {
  def persistedKey: Array[Byte]
  def snapshotKey: Array[Byte]
  def persistedKeyString = new String(persistedKey, "utf-8")
  def snapshotKeyString  = new String(snapshotKey, "utf-8")
}

object BCSKey {
  private val log = LoggerFactory.getLogger(getClass)

  def parseFromSnapshot(bytes: Array[Byte]): Option[BCSKey] = {
    val str = new String(bytes, "utf-8")
    log.debug(s"parse snapshot key: $str")

    MetaKey
      .parseFromSnapshot(str)
      .orElse(BlockKey.parseFromSnapshot(str))
      .orElse(TransactionKey.parseFromSnapshot(str))
      .orElse(ReceiptKey.parseFromSnapshot(str))
      .orElse(StateKey.parseFromSnapshot(str))
      .orElse {
        log.warn(s"can't parse snapshot key: $str")
        None
      }

  }

  private[types] trait BCSUrlKey extends BCSKey {
    val scheme: String
    val segments: Array[String]

    override def persistedKey: Array[Byte] = {
      val infix = "persisted"
      s"$scheme:$infix://${segments.map(URLEncoder.encode(_, "utf-8")).mkString("/")}"
        .getBytes("utf-8")
    }
    override def snapshotKey: Array[Byte] = {
      val infix = "snapshot"
      s"$scheme:$infix://${segments.map(URLEncoder.encode(_, "utf-8")).mkString("/")}"
        .getBytes("utf-8")
    }
  }

  sealed trait MetaKey extends BCSKey
  object MetaKey {
    private abstract class _MetaKey() extends MetaKey with BCSUrlKey {
      override val scheme: String = "meta"
    }

    val ChainID: MetaKey = new _MetaKey {
      override val segments: Array[String] = Array("chainId")
    }
    val Height: MetaKey = new _MetaKey {
      override val scheme: String          = "meta"
      override val segments: Array[String] = Array("height")
    }
    val Version: MetaKey = new _MetaKey {
      override val scheme: String          = "meta"
      override val segments: Array[String] = Array("version")
    }

    def parseFromSnapshot(str: String): Option[MetaKey] = {
      if (str.startsWith("meta:snapshot:")) {
        str.drop("meta:snapshot://".length) match {
          case "chainId" => Some(MetaKey.ChainID)
          case "height"  => Some(MetaKey.Height)
          case "version" => Some(MetaKey.Version)
          case _         => None
        }
      } else None

    }
  }

  sealed trait BlockKey extends BCSKey {
    def height: BigInt
  }
  object BlockKey {
    private abstract class _BlockKey(val height: BigInt) extends BlockKey with BCSUrlKey {
      override val scheme: String = s"block:$height"
    }

    def preWorldState(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("preBlockState")
    }

    def curWorldState(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("preGlobalState")
    }

    def transactionList(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("transactions")
    }

    def receiptList(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("receipts")
    }

    def blockTimestamp(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("timestamp")
    }

    def blockHash(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("hash")
    }

    def blockHeight(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("height")
    }

    def blockChainId(height: BigInt): BlockKey = new _BlockKey(height) {
      override val segments: Array[String] = Array("chainId")
    }

    private val P = ("block:(\\d+):snapshot://(height|chainId|" +
      "preWorldState|curWorldState|transactions|receipts|timestamp|hash)").r
    def parseFromSnapshot(str: String): Option[BlockKey] = {
      //block:{height}:snapshot://preHash
      str match {
        case P(height, key) if key == "height"        => Some(blockChainId(BigInt(height)))
        case P(height, key) if key == "chainId"       => Some(blockChainId(BigInt(height)))
        case P(height, key) if key == "preWorldState" => Some(preWorldState(BigInt(height)))
        case P(height, key) if key == "curWorldState" => Some(preWorldState(BigInt(height)))
        case P(height, key) if key == "transactions"  => Some(transactionList(BigInt(height)))
        case P(height, key) if key == "receipts"      => Some(receiptList(BigInt(height)))
        case P(height, key) if key == "timestamp"     => Some(blockTimestamp(BigInt(height)))
        case P(height, key) if key == "hash"          => Some(blockHash(BigInt(height)))
        case _                                        => None
      }
    }
  }

  sealed trait TransactionKey extends BCSKey {
    def height: BigInt
  }
  object TransactionKey {
    private abstract class _TransactionKey(val height: BigInt)
        extends TransactionKey
        with BCSUrlKey {
      override val scheme: String = s"transaction:$height"
    }

    def transaction(height: BigInt, transactionId: String): TransactionKey =
      new _TransactionKey(height) {
        override val segments: Array[String] = Array(transactionId)
      }
    private val P = "transaction:(\\d+):snapshot://(.+)".r
    def parseFromSnapshot(str: String): Option[TransactionKey] = {
      str match {
        case P(height, transactionId) => Some(transaction(BigInt(height), transactionId))
        case _                        => None
      }
    }
  }

  sealed trait ReceiptKey extends BCSKey {
    def height: BigInt
  }
  object ReceiptKey {
    private abstract class _ReceiptKey(val height: BigInt) extends ReceiptKey with BCSUrlKey {
      override val scheme: String = s"receipt:$height"
    }

    def receiptResult(height: BigInt, transactionId: String): ReceiptKey = new _ReceiptKey(height) {
      override val segments: Array[String] = Array(transactionId, "result")
    }

    def receiptCost(height: BigInt, transactionId: String): ReceiptKey = new _ReceiptKey(height) {
      override val segments: Array[String] = Array(transactionId, "cost")
    }

    def receiptLogs(height: BigInt, transactionId: String): ReceiptKey = new _ReceiptKey(height) {
      override val segments: Array[String] = Array(transactionId, "logs")
    }
    private val P = "receipt:(\\d+):snapshot://(.+)/(result|cost|logs)".r
    def parseFromSnapshot(str: String): Option[ReceiptKey] = {
      str match {
        case P(height, transactionId, key) if key == "result" =>
          Some(receiptResult(BigInt(height), transactionId))
        case P(height, transactionId, key) if key == "cost" =>
          Some(receiptCost(BigInt(height), transactionId))
        case P(height, transactionId, key) if key == "logs" =>
          Some(receiptLogs(BigInt(height), transactionId))
        case _ => None
      }
    }
  }

  sealed trait StateKey extends BCSKey
  object StateKey {
    private abstract class _StateKey(accountId: String) extends StateKey with BCSUrlKey {
      override val scheme: String = "state"
    }

    def balance(accountId: String): StateKey = new _StateKey(accountId) {
      override val segments: Array[String] = Array(accountId, "balance")
    }

    def contractDesc(accountId: String, contractName: String, version: String): StateKey =
      new _StateKey(accountId) {
        override val segments: Array[String] =
          Array(accountId, "contracts", contractName, "versions", version, "desc")
      }

    def contractCode(accountId: String, contractName: String, version: String): StateKey =
      new _StateKey(accountId) {
        override val segments: Array[String] =
          Array(accountId, "contracts", contractName, "versions", version, "code")
      }

    def contractRuntime(accountId: String, contractName: String, version: String): StateKey =
      new _StateKey(accountId) {
        override val segments: Array[String] =
          Array(accountId, "contracts", contractName, "versions", version, "runtime")
      }

    def contractDb(accountId: String, contractName: String, appKey: String): StateKey =
      new _StateKey(accountId) {
        override val segments: Array[String] =
          Array(accountId, "contracts", contractName, "db", URLEncoder.encode(appKey, "utf-8"))
      }

    def contractInvoking(accountId: String, contractName: String): StateKey =
      new _StateKey(accountId) {
        override val segments: Array[String] =
          Array(accountId, "contracts", contractName, "invoke")
      }
    private val BALANCE = "state:snapshot://(.+)/balance".r
    private val CONTRACT_INFO =
      "state:snapshot://(.+)/contracts/(.+)/versions/(.+)/(desc|code|runtime)".r
    private val CONTRACT_DB       = "state:snapshot://(.+)/contracts/(.+)/db/(.+)".r
    private val CONTRACT_INVOKING = "state:snapshot://(.+)/contracts/(.+)/invoke".r

    def parseFromSnapshot(str: String): Option[StateKey] = str match {
      case BALANCE(accountId) => Some(balance(accountId))
      case CONTRACT_INFO(accountId, contractName, version, tag) if tag == "desc" =>
        Some(contractDesc(accountId, contractName, version))
      case CONTRACT_INFO(accountId, contractName, version, tag) if tag == "code" =>
        Some(contractCode(accountId, contractName, version))
      case CONTRACT_INFO(accountId, contractName, version, tag) if tag == "runtime" =>
        Some(contractRuntime(accountId, contractName, version))
      case CONTRACT_DB(accountId, contractName, appKey) =>
        Some(contractDb(accountId, contractName, appKey))
      case CONTRACT_INVOKING(accountId, contractName) =>
        Some(contractInvoking(accountId, contractName))
      case _ => None
    }
  }
}
