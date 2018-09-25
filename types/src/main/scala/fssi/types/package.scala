package fssi

import utils._

package object types {
  type TokenUnit    = Token.Unit
  type UserContract = Contract.UserContract

  object syntax extends BytesValue.Syntax
  object implicits
      extends base.BaseTypeImplicits
      with biz.Account.Implicits
      with biz.Transaction.Implicits
      with biz.Token.Implicits
      with biz.Contract.Version.Implicits
      with biz.Contract.UserContract.Implicits
      with Transaction.Implicits
      with Contract.Implicits
      with Token.Implicits

}
