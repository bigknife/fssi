package fssi.types
package biz

import base._

sealed trait Transaction {
  def id: Transaction.ID
  def sender: Account.ID
  def signature: Signature
  def timestamp: Long
}

object Transaction {
  case class ID(value: Array[Byte])
  def emptyId: ID = ID(Array.emptyByteArray)

  /** Transfer is a  transaction to transfer payer's token to payee's token
    */
  case class Transfer(
      id: Transaction.ID,
      payer: Account.ID,
      payee: Account.ID,
      token: Token,
      signature: Signature,
      timestamp: Long
  ) extends Transaction {
    def sender: Account.ID = payer
  }

  /** Deploy is a transaction to publish a smart contract to the block chain
    */
  case class Deploy(
      id: Transaction.ID,
      owner: Account.ID,
      contract: Contract.UserContract,
      signature: Signature,
      timestamp: Long
  ) extends Transaction {
    def sender: Account.ID = owner
  }

  /** Run is a transaction to run a smart contract on the block chain
    */
  case class Run(
    id: Transaction.ID,
    caller: Account.ID,
    contractName: UniqueName,
    contractVersion: Contract.Version,
    methodAlias: String,
    contractParameter: Option[Contract.UserContract.Parameter],
    signature: Signature,
    timestamp: Long
  ) extends Transaction {
    def sender: Account.ID = caller
  }


  /** Implicits */
  trait Implicits {
    implicit def transactionIdToBytesValue(id: ID): Array[Byte] = id.value
  }
}
