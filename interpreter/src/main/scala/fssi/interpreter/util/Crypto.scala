package fssi.interpreter.util

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.{KeyFactory, KeyPairGenerator, SecureRandom, Security, Signature => JSS}
import java.util.UUID

import fssi.ast.domain.types._
import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECPrivateKeySpec, ECPublicKeySpec}

/**
  * put all cryptography operations together
  */
trait Crypto {
  // ECDSA
  //       ref: http://www.bouncycastle.org/wiki/display/JA1/Elliptic+Curve+Key+Pair+Generation+and+Key+Factories
  //            http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
  val ECSpec: String               = "prime256v1"
  val KeyPairAlgorithm: String     = "ECDSA"
  val ProviderName: String         = "BC"
  val SecretKeyFactoryAlgo: String = "desede"
  val CipherAlgo: String           = "desede/CBC/PKCS5Padding"
  val SignAlgo: String             = "SHA256withECDSA"
  val KeyFactoryAlgo: String       = "ECDH"

  // init bcprovider
  if (!Security.getProviders.exists(_.getName == ProviderName)) {
    Security.addProvider(new BouncyCastleProvider)
  }

  def generateKeyPair(): KeyPair = {
    val ecSpec = ECNamedCurveTable.getParameterSpec(ECSpec)
    val g      = KeyPairGenerator.getInstance(KeyPairAlgorithm, ProviderName)
    g.initialize(ecSpec, new SecureRandom())
    KeyPair.fromJCEKeyPair(g.generateKeyPair())
  }

  def randomChar(len: Int): Array[Char] = {
    import scala.util._

    @scala.annotation.tailrec
    def loop(i: Int, acc: Vector[Char]): Vector[Char] =
      if (i == 0) acc
      else {
        val newChar: Char = (Random.nextInt(26) + 97).toChar
        loop(i - 1, acc :+ newChar)
      }
    loop(len, Vector.empty).toArray[Char]
  }

  def enforceDes3Key(key: BytesValue): BytesValue = {
    key match {
      case x if x.bytes.length == 24 => x
      case x if x.bytes.length < 24  => BytesValue(ByteBuffer.allocate(24).put(x.bytes).array())
      case x                         => BytesValue(ByteBuffer.allocate(24).put(x.bytes, 24, 0).array())
    }
  }

  def des3cbcEncrypt(source: BytesValue, key: BytesValue, iv: BytesValue): BytesValue = {
    val spec       = new DESedeKeySpec(key.bytes)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv.bytes)
    cipher.init(Cipher.ENCRYPT_MODE, desKey, ivSpec)

    BytesValue(cipher.doFinal(source.bytes))
  }

  def randomUUID(): String = {
    UUID.randomUUID().toString.replaceAll("-", "").toUpperCase
  }

  def validateSignature(sign: Signature, source: BytesValue, publ: KeyPair.Publ): Boolean = {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initVerify(publ.publicKey)
    s.update(source.bytes)
    s.verify(sign.bytes)
  }

  def makeSignature(source: BytesValue, priv: KeyPair.Priv): BytesValue = {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initSign(priv.privateKey)
    s.update(source.bytes)
    BytesValue(s.sign())
  }

  def privateKeyData(priv: KeyPair.Priv): BytesValue = {
    BytesValue(priv.privateKey.asInstanceOf[ECPrivateKey].getD.toByteArray)
  }

  def publicKeyData(publ: KeyPair.Publ): BytesValue = {
    BytesValue(publ.publicKey.asInstanceOf[ECPublicKey].getQ.getEncoded(true))
  }

  def rebuildPubl(bytesValue: BytesValue): KeyPair.Publ = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val pubSpec = new ECPublicKeySpec(ecSpec.getCurve.decodePoint(bytesValue.bytes), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    KeyPair.Publ(kf.generatePublic(pubSpec))
  }

  def rebuildPriv(bytesValue: BytesValue): KeyPair.Priv = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val prvSpec = new ECPrivateKeySpec(new BigInteger(bytesValue.bytes), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    KeyPair.Priv(kf.generatePrivate(prvSpec))
  }

  def des3cbcDecrypt(source: BytesValue, key: BytesValue, iv: BytesValue): BytesValue = {
    val spec       = new DESedeKeySpec(key.bytes)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv.bytes)
    cipher.init(Cipher.DECRYPT_MODE, desKey, ivSpec)
    BytesValue(cipher.doFinal(source.bytes))
  }

  def hash(source: BytesValue): BytesValue = {
    BytesValue(sha3(source.bytes))
  }

  def sha3(s: Array[Byte]): Array[Byte] = {
    import org.bouncycastle.jcajce.provider.digest.SHA3
    new SHA3.Digest256().digest(s)
  }

}