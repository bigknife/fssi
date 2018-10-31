package fssi
package interpreter

import types._
import utils._
import ast._
import fssi.types.base.{Base58Check, Hash, RandomSeed, Signature}
import fssi.types.biz.Contract.UserContract
import fssi.types.biz.{Account, Block, Transaction}
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
    val kp        = crypto.generateECKeyPair(crypto.SECP256K1)
    val privBytes = crypto.getECPrivateKey(kp)
    val pubBytes  = crypto.getECPublicKey(kp)
    Account.KeyPair(privKey = Account.PrivKey(privBytes), pubKey = Account.PubKey(pubBytes))
  }

  /** create a secret key to encrypt private key of account
    */
  override def createSecretKey(rnd: RandomSeed): Stack[Account.SecretKey] = Stack {
    val secretKeyBytes = crypto.createAesSecretKey(rnd.value)
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

  override def verifyTransactionSignature(transaction: Transaction): Stack[Signature.VerifyResult] =
    ???

  override def verifyBlockHash(block: Block): Stack[Hash.VerifyResult] = Stack {
    val blockBytes = calculateUnsignedBlockBytes(block)
    val hash       = Hash(crypto.hash(blockBytes))
    if (hash === block.hash) Hash.Passed else Hash.Tampered
  }

  override def makeTransactionSignature(transaction: Transaction,
                                        privateKey: Account.PrivKey): Stack[Signature] = Stack {
    val source = calculateUnsignedTransactionBytes(transaction)
    val signedBytes =
      crypto.makeSignature(source, crypto.rebuildECPrivateKey(privateKey.value, crypto.SECP256K1))
    Signature(signedBytes)
  }

  override def makeContractSignature(contract: UserContract,
                                     privateKey: Account.PrivKey): Stack[Signature] = Stack {
    val source = calculateContractBytes(contract)
    val signedBytes =
      crypto.makeSignature(source, crypto.rebuildECPrivateKey(privateKey.value, crypto.SECP256K1))
    Signature(signedBytes)
  }

  private def calculateUnsignedBlockBytes(block: Block): Array[Byte] =
    block.head.asBytesValue.bytes ++ block.transactions.toArray.asBytesValue.bytes ++ block.receipts.toArray.asBytesValue.bytes

  private def calculateUnsignedTransactionBytes(transaction: Transaction): Array[Byte] = {
    transaction match {
      case Transaction.Transfer(id, payer, payee, token, _, timestamp) =>
        id.asBytesValue.bytes ++ payer.asBytesValue.bytes ++ payee.asBytesValue.bytes ++ token.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
      case Transaction.Deploy(id, owner, contract, _, timestamp) =>
        id.asBytesValue.bytes ++ owner.asBytesValue.bytes ++ contract.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
      case Transaction.Run(id,
                           caller,
                           contractName,
                           contractVersion,
                           methodAlias,
                           contractParameter,
                           _,
                           timestamp) =>
        id.asBytesValue.bytes ++ caller.asBytesValue.bytes ++ contractName.asBytesValue.bytes ++ contractVersion.asBytesValue.bytes ++ methodAlias.asBytesValue.bytes ++ contractParameter.asBytesValue.bytes ++ timestamp.asBytesValue.bytes
    }
  }

  private def calculateContractBytes(contract: UserContract): Array[Byte] = {
    import contract._
    owner.asBytesValue.bytes ++ name.asBytesValue.bytes ++ version.asBytesValue.bytes ++ code.asBytesValue.bytes ++ methods.toArray.asBytesValue.bytes
  }
}

object CryptoHandler {
  val instance = new CryptoHandler
  trait Implicits {
    implicit val cryptoHandlerInstance: CryptoHandler = instance
  }

  object implicits extends Implicits
}
