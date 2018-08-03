package fssi.types

/**
  * Transaction is the activities happened on the chain, such as:
  * 1. transfer token to others
  * 2. publish a smart contract
  * 3. run a smart contract
  *
  * Transaction is the description of the activity, so it's determined, there is
  * some properties:
  *
  * 1. id the unique id of a transaction
  * 2. sender someone who sent this transaction to the chain
  * 3. signature used to prove that this transaction was sent by someone
  * 4. timestamp the timestamp when the transaction was sent
  */
sealed trait Transaction {
  def id: Transaction.ID
  def sender: Account.ID
  def signature: Signature
  def timestamp: Long
}

object Transaction {
  case class ID(value: String)

  /**
    * Transfer is a kind of Transaction, through which, we can transfer
    * token of our own account to other account.
    */
  case class Transfer(
      id: Transaction.ID,
      from: Account.ID,
      to: Account.ID,
      token: Token,
      signature: Signature,
      timestamp: Long
  ) extends Transaction {
    val sender: Account.ID = from
  }

  /**
    * Publishcontract is a kind of Transaction, through which, we can publish
    * a smart contract onto the chain.
    */
  case class PublishContract(
      id: Transaction.ID,
      owner: Account.ID,
      contract: Contract.UserContract,
      signature: Signature,
      timestamp: Long
  ) extends Transaction {
    val sender: Account.ID = owner
  }

  /**
    * RunContract is a kind of Transaction, through which, we can run a contract
    * with proper arguments to change the state of the chain
    */
  case class RunContract(
      id: Transaction.ID,
      sender: Account.ID,
      contractName: UniqueName,
      contractVersion: Version,
      contractMethod: Contract.Method,
      contractParameter: Contract.Parameter,
      signature: Signature,
      timestamp: Long
  ) extends Transaction
}
