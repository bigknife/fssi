package fssi.contract

/** the state of an account
  *
  * @param accountId id of an account
  * @param amount amount of an account
  * @param assets assets of an account
  */
case class AccountState(accountId: String, amount: BigDecimal, assets: Map[String, Array[Byte]]) {
  def withAssets(_assets: Map[String, Array[Byte]]): AccountState = copy(assets = _assets)
  def withAmount(_amount: BigDecimal): AccountState = copy(amount = _amount)

  def assetOf(key: String): Option[Array[Byte]] = assets.get(key)

  def updateAsset(key: String, data: Array[Byte]): AccountState = copy(assets = assets + (key -> data))


}

object AccountState {
  def emptyFor(accountId: String): AccountState = AccountState(accountId, BigDecimal(0), Map.empty)
}
