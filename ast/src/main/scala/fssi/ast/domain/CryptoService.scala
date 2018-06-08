package fssi.ast.domain

import bigknife.sop._
import macros._
import implicits._
import fssi.ast.domain.types.{BytesValue, KeyPair, Signature}

/** basic operation of cryptography
  */
@sp trait CryptoService[F[_]] {

  /**
    * generate a pair of private key and public key
    * @return
    */
  def generateKeyPair(): P[F, KeyPair]

  def privateKeyData(priv: KeyPair.Priv): P[F, BytesValue]
  def publicKeyData(publ: KeyPair.Publ): P[F, BytesValue]

  def rebuildPubl(bytesValue: BytesValue): P[F, KeyPair.Publ]
  def rebuildPriv(bytesValue: BytesValue): P[F, KeyPair.Priv]

  /**
    * encrypt source data
    * @param source source data
    * @param key the key used to encrypt
    * @param iv iv of des3cbc
    * @return data encrypted from the source
    */
  def des3cbcEncrypt(source: BytesValue, key: BytesValue, iv: BytesValue): P[F, BytesValue]

  /**
    * decrypt source data
    * @param source encrypted data
    * @param key the key used to decrypt sourc
    * @param iv iv of des3cbc
    * @return data decrypted from source
    */
  def des3cbcDecrypt(source: BytesValue, key: BytesValue, iv: BytesValue): P[F, BytesValue]

  /** des3 key size should be 24bytes
    *
    * @param key key that the size may be not 24
    * @return key that the size MUST be 24
    */
  def enforceDes3Key(key: BytesValue): P[F, BytesValue]

  /**
    * generate chars randomly
    * @param len length of chars
    * @return chars
    */
  def randomChar(len: Int): P[F, Array[Char]]
  def randomByte(len: Int): P[F, BytesValue] =
    randomChar(len).map(_.map(x => x.toByte)).map(BytesValue.apply)

  /** create a random uuid
    *
    * @return 32Bytes uuid string, no `-`.
    */
  def randomUUID(): P[F, String]

  /** validate a sign by using a public key, if passed return true, or false */
  def validateSignature(sign: Signature, source: BytesValue, publ: KeyPair.Publ): P[F, Boolean]

  /** make a signature */
  def makeSignature(source: BytesValue, priv: KeyPair.Priv): P[F, BytesValue]

  /** make a hash */
  def hash(source: BytesValue): P[F, BytesValue]
  
}
