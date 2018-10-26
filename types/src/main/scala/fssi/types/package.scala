package fssi

import utils._

package object types {

  type TransactionSet = scala.collection.immutable.TreeSet[biz.Transaction]
  object TransactionSet {
    def empty: TransactionSet = scala.collection.immutable.TreeSet.empty[biz.Transaction]
  }

  object syntax extends BytesValue.Syntax
  object implicits
      extends base.BaseTypeImplicits
      with biz.Account.Implicits
      with biz.Transaction.Implicits
      with biz.Token.Implicits
      with biz.Contract.Version.Implicits
      with biz.Contract.UserContract.Implicits
      with biz.Block.Implicits
}
