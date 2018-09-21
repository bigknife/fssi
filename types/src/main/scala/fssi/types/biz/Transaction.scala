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
    val sender: Account.ID = payer
  }

  /** Deploy is a transaction to publish a smart contract to the block chain
    */


  /** Implicits */
  trait Implicits {
    implicit def transactionIdToBytesValue(id: ID): Array[Byte] = id.value
  }
}
