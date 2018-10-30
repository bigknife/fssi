package fssi.ast

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.biz._
import fssi.types.base._

@sp trait Crypto[F[_]] {

  /** create random seed
    */
  def createRandomSeed(): P[F, RandomSeed]

  /** create keypair for an account
    */
  def createAccountKeyPair(): P[F, Account.KeyPair]

  /** create a secret key to encrypt private key of account
    */
  def createSecretKey(rnd: RandomSeed): P[F, Account.SecretKey]

  /** create initial vector for an account, which is used to encrypt private key of account
    */
  def createAccountIV(): P[F, Account.IV]

  /** encrypt private key of account
    */
  def encryptAccountPrivKey(privKey: Account.PrivKey,
                            sk: Account.SecretKey,
                            iv: Account.IV): P[F, Account.PrivKey]

  /** decrypt private key of account
    */
  def decryptAccountPrivKey(encPrivKey: Account.PrivKey,
                            sk: Account.SecretKey,
                            iv: Account.IV): P[F, Account.PrivKey]

  /** create an account id compatible to an account of btc.
    * double hash and wrapped into Base58check.
    */
  def createAccountID(pubKey: Account.PubKey): P[F, Account.ID]

  def verifyTransactionSignature(transaction: Transaction): P[F, Signature.VerifyResult]

  def verifyBlockHash(block: Block): P[F, Hash.VerifyResult]
}
