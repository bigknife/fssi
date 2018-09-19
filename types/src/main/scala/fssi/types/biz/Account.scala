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
  case class PrivKey(value: Array[Byte])
  case class PubKey(value: Array[Byte])
  case class ID(value: Array[Byte])
  case class IV(value: Array[Byte])

  /** secretkey is used to encrypt the private key
    */
  case class SecretKey(value: Array[Byte])

  trait Implicits {
    implicit def accountPrivateKeyToBytesValue(x: Account.PrivKey): Array[Byte]  = x.value
    implicit def accountPubKeyToBytesValue(x: Account.PubKey): Array[Byte]       = x.value
    implicit def accountIDToBytesValue(x: Account.ID): Array[Byte]               = x.value
    implicit def accountSecretKeyToBytesValue(x: Account.SecretKey): Array[Byte] = x.value
    implicit def accountIVToBytesValue(x: Account.IV): Array[Byte]               = x.value
  }
}
