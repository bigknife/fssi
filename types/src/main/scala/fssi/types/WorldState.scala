package fssi.types

/**
  * The highest level of  WorldState abstraction
  */
trait WorldState {

  /** Get token of an account in this state
    * if no token found in this state, a empty token should be returned.
    */
  def getToken(accountId: Account.ID): Option[Token]

  /** Update token for an account in this state */
  def setToken(accountId: Account.ID, token: Token): WorldState

  /** Get someone's asset data in this state */
  def getAssetData(accountId: Account.ID, assetName: UniqueName): Option[DataBlock.Value]

  /** Update asset data of an account */
  def setAssetData(accountId: Account.ID, assetName: UniqueName, value: DataBlock.Value): WorldState

  /**
    * Get contract data attached to a specific key,
    * if the key is not found, an empty value should be returned.
    */
  def getContractData(contractName: UniqueName, key: DataBlock.Key): Option[DataBlock.Value]

  /**
    * Update contract value of a key.
    */
  def setContractData(contractName: UniqueName, key: DataBlock.Key, value: DataBlock.Value): WorldState
}
