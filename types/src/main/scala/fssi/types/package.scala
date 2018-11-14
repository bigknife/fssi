package fssi

import fssi.types.biz._

package object types {
  type ConsensusMessage   = biz.Message.ConsensusMessage
  type ApplicationMessage = biz.Message.ApplicationMessage
  type ClientMessage      = biz.Message.ClientMessage

  type ServiceResource = () => Unit

  type TransactionSet = scala.collection.immutable.TreeSet[biz.Transaction]
  object TransactionSet {
    def empty: TransactionSet = scala.collection.immutable.TreeSet.empty[biz.Transaction]
    def apply(transactions: Transaction*): TransactionSet =
      scala.collection.immutable.TreeSet(transactions: _*)
  }

  type ReceiptSet = scala.collection.immutable.TreeSet[biz.Receipt]
  object ReceiptSet {
    def empty: ReceiptSet =  scala.collection.immutable.TreeSet.empty[biz.Receipt]
    def apply(receipts: Receipt*): ReceiptSet =
      scala.collection.immutable.TreeSet(receipts: _*)
  }

  //object syntax extends BytesValue.Syntax
  object implicits
      extends fssi.types.base.BaseTypeImplicits
      with biz.Account.Implicits
      with biz.Transaction.Implicits
      with biz.Token.Implicits
      with biz.Contract.Version.Implicits
      with biz.Contract.UserContract.Implicits
      with biz.Block.Implicits
      with biz.Receipt.Implicits
}
