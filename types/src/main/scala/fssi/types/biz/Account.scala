package fssi.types
package biz

import base._

/** Account, a crypto digital account, which is compatible with Btc account
  *
  */
case class Account(
    encPrivKey: Account.PrivKey,
    pubKey: Account.PubKey,
    iv: Account.IV,
    id: Account.ID
)

object Account {
  case class PrivKey(value: Array[Byte])   extends AnyVal {
    def ===(that: PrivKey): Boolean = value sameElements that.value
  }
  case class PubKey(value: Array[Byte])    extends AnyVal {
    def ===(that: PubKey): Boolean = value sameElements that.value
  }
  case class ID(value: Array[Byte])        extends AnyVal {
    def ===(that: ID): Boolean = value sameElements that.value
  }
  case class IV(value: Array[Byte])        extends AnyVal {
    def ===(that: IV): Boolean = value sameElements that.value
  }
  case class SecretKey(value: Array[Byte]) extends AnyVal {
    def ===(that: SecretKey): Boolean = value sameElements that.value
  }

  case class KeyPair(privKey: PrivKey, pubKey: PubKey)

  def emptyId: ID = ID(Array.emptyByteArray)

  trait Implicits {
    implicit def accountPrivateKeyToBytesValue(x: Account.PrivKey): Array[Byte]  = x.value
    implicit def accountPubKeyToBytesValue(x: Account.PubKey): Array[Byte]       = x.value
    implicit def accountIDToBytesValue(x: Account.ID): Array[Byte]               = x.value
    implicit def accountSecretKeyToBytesValue(x: Account.SecretKey): Array[Byte] = x.value
    implicit def accountIVToBytesValue(x: Account.IV): Array[Byte]               = x.value
  }
}
