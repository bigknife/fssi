package fssi
package ast

import types._,exception._, base._
import utils._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

/**
  * Cryptography operations
  */
@sp trait Crypto[F[_]] {

  /**
    * Create a public/private key pair
    * @return a pair of bytes value
    *         the first element is public key, the second element is private key.
    */
  def createKeyPair(): P[F, (OpaqueBytes, OpaqueBytes)]

  /**
    * Create IV(initialization vector) fork Des encrypt
    * @return iv data represented by base64string
    */
  def createIVForDes(): P[F, OpaqueBytes]

  /** encrypt a private key by using iv and password
    *
    * @return encrypted private key, represented by base64string.
    */
  def desEncryptPrivateKey(privateKey: OpaqueBytes,
                           iv: OpaqueBytes,
                           password: OpaqueBytes): P[F, OpaqueBytes]

  /** decrypt a encrypted private key by using iv and password
    */
  def desDecryptPrivateKey(encryptedPrivateKey: OpaqueBytes,
                           iv: OpaqueBytes,
                           password: OpaqueBytes): P[F, Either[FSSIException, OpaqueBytes]]

  /** make a signature for source bytes by using a private key
    */
  def makeSignature(source: OpaqueBytes, privateKey: OpaqueBytes): P[F, Signature]

  /** verify signature
    */
  def verifySignature(source: OpaqueBytes, publicKey: OpaqueBytes, signature: Signature): P[F, Boolean]
}
