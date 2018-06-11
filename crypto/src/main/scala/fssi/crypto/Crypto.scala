package fssi.crypto

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.{KeyFactory, KeyPair, KeyPairGenerator, PrivateKey, PublicKey, SecureRandom, Security, Signature => JSS}
import java.util.UUID

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
    g.generateKeyPair()
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

  def enforceDes3Key(key: Array[Byte]): Array[Byte] = {
    key match {
      case x if x.length == 24 => x
      case x if x.length < 24  => ByteBuffer.allocate(24).put(x).array()
      case x                         => ByteBuffer.allocate(24).put(x, 24, 0).array()
    }
  }

  def des3cbcEncrypt(source: Array[Byte], key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(key)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, desKey, ivSpec)

    cipher.doFinal(source)
  }

  def randomUUID(): String = {
    UUID.randomUUID().toString.replaceAll("-", "").toUpperCase
  }

  def validateSignature(sign: Array[Byte], source: Array[Byte], publ: PublicKey): Boolean = {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initVerify(publ)
    s.update(source)
    s.verify(sign)
  }

  def makeSignature(source: Array[Byte], priv: PrivateKey): Array[Byte] = {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initSign(priv)
    s.update(source)
    s.sign()
  }

  def privateKeyData(priv: PrivateKey): Array[Byte] = {
    priv.asInstanceOf[ECPrivateKey].getD.toByteArray
  }

  def publicKeyData(publ: PublicKey): Array[Byte] = {
    publ.asInstanceOf[ECPublicKey].getQ.getEncoded(true)
  }

  def rebuildPubl(bytesValue: Array[Byte]): PublicKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val pubSpec = new ECPublicKeySpec(ecSpec.getCurve.decodePoint(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePublic(pubSpec)
  }

  def rebuildPriv(bytesValue: Array[Byte]): PrivateKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val prvSpec = new ECPrivateKeySpec(new BigInteger(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePrivate(prvSpec)
  }

  def des3cbcDecrypt(source: Array[Byte], key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(key)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, desKey, ivSpec)
    cipher.doFinal(source)
  }

  def hash(source: Array[Byte]): Array[Byte] = {
    sha3(source)
  }

  def sha3(s: Array[Byte]): Array[Byte] = {
    import org.bouncycastle.jcajce.provider.digest.SHA3
    new SHA3.Digest256().digest(s)
  }

}