package fssi.types
package biz

import base._
import fssi.types.implicits._

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

    implicit val bizTransactionOrdering: Ordering[Transaction] = new Ordering[Transaction] {
      def compare(t1: Transaction, t2: Transaction): Int = {
        val o1 = Ordering[Long].compare(t1.timestamp, t2.timestamp)
        if (o1 != 0) {
          Ordering[String].compare(t1.id.asBytesValue.bcBase58, t2.id.asBytesValue.bcBase58)
        } else o1
      }
    }

    implicit def bizTransferToBytesValue(a: Transfer): Array[Byte] = {
      import a._
      (id.asBytesValue.any ++ payer.asBytesValue.any ++ payee.asBytesValue.any ++
        token.asBytesValue.any ++ signature.asBytesValue.any ++ timestamp.asBytesValue.any).bytes
    }

    implicit def bizDeployToBytesValue(a: Deploy): Array[Byte] = {
      import a._
      (id.asBytesValue.any ++ owner.asBytesValue.any ++ contract.asBytesValue.any ++
        signature.asBytesValue.any ++ timestamp.asBytesValue.any).bytes
    }

    implicit def bizRunToBytesValue(a: Run): Array[Byte] = {
      import a._
      (id.asBytesValue.any ++ caller.asBytesValue.any ++ contractName.asBytesValue.any ++
        contractVersion.asBytesValue.any ++ methodAlias.asBytesValue.any ++ contractParameter.asBytesValue.any ++
        signature.asBytesValue.any ++ timestamp.asBytesValue.any).bytes
    }

    implicit def bizTransactionToBytesValue(a: Transaction): Array[Byte] = a match {
      case x: Transfer => bizTransactionToBytesValue(x)
      case x: Deploy => bizDeployToBytesValue(x)
      case x: Run => bizRunToBytesValue(x)
    }
  }
}
