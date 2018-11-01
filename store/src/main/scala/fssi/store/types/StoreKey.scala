package fssi.store
package types

sealed trait StoreKey extends Ordered[StoreKey]{
  def stringValue: String
  def previousLevel: Option[StoreKey]
  private[store] def withTag(tag: String): StoreKey

  override def toString: String = stringValue
  def compare(that: StoreKey): Int = Ordering[String].compare(stringValue, that.stringValue)
  def ===(that: StoreKey): Boolean = stringValue == that.stringValue
  def bytesValue: Array[Byte] = stringValue.getBytes("utf-8")
}

object StoreKey {
  private[types] case class Segmented(segments: Array[String], tag: Option[String] = None) extends StoreKey {
    private[store] def withTag(tag: String): StoreKey = copy(tag = Some(tag))
    def nextLevel(s: String): Segmented =
      Segmented(segments :+ java.net.URLEncoder.encode(s, "utf-8"))
    def previousLevel: Option[StoreKey] =
      if (segments.length <= 1) None
      else Some(Segmented(segments.dropRight(1)))

    def stringValue: String = segments.mkString("/") + tag.map("#" + _).getOrElse("")
  }



  private def _meta: Segmented = Segmented(Array("meta:"))

  def parse(s: String): StoreKey = {
    val segs = s.split("\\/")
    // last seg contains '#'? that's tag
    if (segs.nonEmpty) {
      val lastWithHash = segs.last.split("#")
      if (lastWithHash.length > 1) {
        val last = lastWithHash(0)
        val tag = lastWithHash(1)
        Segmented(segs.dropRight(1) :+ last, Some(tag))
      }
      else Segmented(segs)
    }
    else Segmented(Array.empty[String])
  }

  def meta: StoreKey = _meta

  def metaChainId: StoreKey =
    _meta.nextLevel("chainId")

  def metaHeight: StoreKey =
    _meta.nextLevel("height")

  def metaVersion: StoreKey =
    _meta.nextLevel("version")

  private def _block: Segmented = Segmented(Array("block:"))

  def block: StoreKey = _block

  def blockHeight(height: BigInt): StoreKey = _block.nextLevel(s"$height")

  def blockPreHash(height: BigInt): StoreKey = _block.nextLevel(s"$height").nextLevel("preHash")

  def blockCurHash(height: BigInt): StoreKey = _block.nextLevel(s"$height").nextLevel("curHash")

  def blockState(height: BigInt): StoreKey = _block.nextLevel(s"$height").nextLevel("state")

  def blockReceipt(height: BigInt): StoreKey = _block.nextLevel(s"$height").nextLevel("receipt")

  def blockTransactions(height: BigInt): StoreKey =
    _block.nextLevel(s"$height").nextLevel("transactions")

  def blockTransaction(height: BigInt, transactionId: String): StoreKey =
    _block.nextLevel(s"$height").nextLevel("transactions").nextLevel(transactionId)

  private def _receipt: Segmented = Segmented(Array("receipt:"))

  def receipt: StoreKey = _receipt

  def receiptHeight(height: BigInt): StoreKey = _receipt.nextLevel(s"$height")

  def receiptTransaction(height: BigInt, transactionId: String): StoreKey =
    _receipt.nextLevel(s"$height").nextLevel(s"$transactionId")

  def receiptTransactionResult(height: BigInt, transactionId: String): StoreKey =
    _receipt.nextLevel(s"$height").nextLevel(s"$transactionId").nextLevel("result")

  def receiptTransactionCost(height: BigInt, transactionId: String): StoreKey =
    _receipt.nextLevel(s"$height").nextLevel(s"$transactionId").nextLevel("cost")

  def receiptTransactionLogs(height: BigInt, transactionId: String): StoreKey =
    _receipt.nextLevel(s"$height").nextLevel(s"$transactionId").nextLevel("logs")

  private def _state: Segmented = Segmented(Array("state:"))

  def state: StoreKey = _state

  def stateAccount(accountId: String): StoreKey = _state.nextLevel(s"$accountId")

  def stateBalance(accountId: String): StoreKey =
    _state.nextLevel(s"$accountId").nextLevel("balance")

  def stateContracts(accountId: String): StoreKey =
    _state.nextLevel(s"$accountId").nextLevel("contracts")

  def stateContract(accountId: String, contractName: String): StoreKey =
    _state.nextLevel(s"$accountId").nextLevel("contracts").nextLevel(s"$contractName")

  def stateContractVersions(accountId: String, contractName: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("versions")

  def stateContractVersion(accountId: String, contractName: String, version: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("versions")
      .nextLevel(s"$version")

  def stateContractVersionDesc(accountId: String, contractName: String, version: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("versions")
      .nextLevel(s"$version")
      .nextLevel("desc")

  def stateContractVersionCode(accountId: String, contractName: String, version: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("versions")
      .nextLevel(s"$version")
      .nextLevel("code")

  def stateContractVersionRuntime(accountId: String,
                                  contractName: String,
                                  version: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("versions")
      .nextLevel(s"$version")
      .nextLevel("runtime")

  def stateContractDb(accountId: String, contractName: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("db")

  def stateContractInvoke(accountId: String, contractName: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("invoke")

  def stateContractDbKey(accountId: String, contractName: String, appKey: String): StoreKey =
    _state
      .nextLevel(s"$accountId")
      .nextLevel("contracts")
      .nextLevel(s"$contractName")
      .nextLevel("db")
      .nextLevel(appKey)

}
