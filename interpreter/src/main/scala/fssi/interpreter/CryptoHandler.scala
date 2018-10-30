package fssi
package interpreter

import types._
import utils._
import ast._
import fssi.types.base.{Base58Check, RandomSeed}
import fssi.types.biz.Account
import types.implicits._

/**
  * CryptoHandler uses ECDSA
  * ECDSA
  *      ref: http://www.bouncycastle.org/wiki/display/JA1/Elliptic+Curve+Key+Pair+Generation+and+Key+Factories
  *           http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
  */
class CryptoHandler extends Crypto.Handler[Stack] with LogSupport {

  crypto.registerBC()

  /** create random seed
    */
  override def createRandomSeed(): Stack[RandomSeed] = Stack {
    val bytes = crypto.randomBytes(24)
    RandomSeed(bytes)
  }

  /** create keypair for an account
    */
  override def createAccountKeyPair(): Stack[Account.KeyPair] = Stack {
    val kp        = crypto.generateECKeyPair(crypto.ECSpec)
    val privBytes = crypto.getECPrivateKey(kp)
    val pubBytes  = crypto.getECPublicKey(kp)
    Account.KeyPair(privKey = Account.PrivKey(privBytes), pubKey = Account.PubKey(pubBytes))
  }

  /** create a secret key to encrypt private key of account
    */
  override def createSecretKey(rnd: RandomSeed): Stack[Account.SecretKey] = Stack {
    val ensureBytes    = crypto.ensure24Bytes(rnd.value)
    val secretKeyBytes = crypto.createDESSecretKey(ensureBytes)
    Account.SecretKey(secretKeyBytes)
  }

  /** create initial vector for an account, which is used to encrypt private key of account
    */
  override def createAccountIV(): Stack[Account.IV] = Stack {
    @scala.annotation.tailrec
    def loop(i: Int, acc: Vector[Char]): Vector[Char] =
      if (i == 0) acc
      else {
        val newChar: Char = (scala.util.Random.nextInt(26) + 97).toChar
        loop(i - 1, acc :+ newChar)
      }
    val bytes = loop(8, Vector.empty).map(_.toByte).toArray
    Account.IV(bytes)
  }

  /** encrypt private key of account
    */
  override def encryptAccountPrivKey(privKey: Account.PrivKey,
                                     sk: Account.SecretKey,
                                     iv: Account.IV): Stack[Account.PrivKey] = Stack {
    val ensuredBytes       = crypto.ensure24Bytes(sk.value)
    val encryptPriKeyBytes = crypto.des3cbcEncrypt(privKey.value, ensuredBytes, iv.value)
    Account.PrivKey(encryptPriKeyBytes)
  }

  /** decrypt private key of account
    */
  override def decryptAccountPrivKey(encPrivKey: Account.PrivKey,
                                     sk: Account.SecretKey,
                                     iv: Account.IV): Stack[Account.PrivKey] = Stack {
    val ensuredBytes       = crypto.ensure24Bytes(sk.value)
    val decryptPriKeyBytes = crypto.des3cbcDecrypt(encPrivKey.value, ensuredBytes, iv.value)
    Account.PrivKey(decryptPriKeyBytes)
  }

  /** create an account id compatible to an account of btc.
    * double hash and wrapped into Base58check.
    */
  override def createAccountID(pubKey: Account.PubKey): Stack[Account.ID] = Stack {
    val idBytes     = crypto.ripemd160(crypto.sha256(pubKey.value))
    val base58Check = Base58Check(0, idBytes).resetChecksum
    Account.ID(base58Check.asBytesValue.bytes)
  }

}

object CryptoHandler {
  val instance = new CryptoHandler
  trait Implicits {
    implicit val cryptoHandlerInstance: CryptoHandler = instance
  }

  object implicits extends Implicits
}
