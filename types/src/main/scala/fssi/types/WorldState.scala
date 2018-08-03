package fssi.types

/**
  * The highest level of  WorldState abstraction
  */
trait WorldState {

  /** Hash of current world state */
  def currentHash: Hash

  /** Hash of previous world state, with this, the state is chained. */
  def previousHash: Hash

  /** Get token of an account in this state
    * if no token found in this state, a empty token should be returned.
    */
  def getToken(accountId: Account.ID): Token

  /** Update token for an account in this state */
  def setToken(accountId: Account.ID, token: Token): WorldState

  /** Get someone's asset data in this state */
  def getAssetData(accountId: Account.ID, assetName: UniqueName): DataBlock.Value

  /** Update asset data of an account */
  def setAssetData(accountId: Account.ID, assetName: UniqueName, value: DataBlock.Value): WorldState

  /**
    * Get contract data attached to a specific key,
    * if the key is not found, an empty value should be returned.
    */
  def getContractData(contractName: UniqueName, key: DataBlock.Key): DataBlock.Value

  /**
    * Update contract value of a key.
    */
  def setContractData(contractName: UniqueName, key: DataBlock.Key, value: DataBlock.Value): WorldState
}
