package fssi

package object types {
  type TokenUnit = Token.Unit
  type UserContract = Contract.UserContract

  object syntax extends BytesValue.Syntax
  object implicits extends Transaction.Implicits
      with Contract.Implicits
  
}
