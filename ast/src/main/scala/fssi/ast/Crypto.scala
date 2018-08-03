package fssi
package ast

import types._
import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

/**
  * Cryptography operations
  */
@sp trait Crypto[F[_]] {

  /**
    * Create a public/private key pair
    * @return a pair of base64string,
    *         the first element is public key, the second element is private key.
    */
  def createKeyPair(): P[F, (BytesValue, BytesValue)]

  /**
    * Create IV(initialization vector) fork Des encrypt
    * @return iv data represented by base64string
    */
  def createIVForDes(): P[F, BytesValue]

  /**
    * encrypt a private key by using iv and password
    * @return encrypted private key, represented by base64string.
    */
  def desEncryptPrivateKey(privateKey: BytesValue,
                           iv: BytesValue,
                           password: BytesValue): P[F, BytesValue]
}
