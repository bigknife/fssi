package fssi.utils

import org.bouncycastle.jcajce.provider.digest.SHA3
import java.security._

import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import java.math.BigInteger

import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}

trait CryptoUtil {
  Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

  // ECDSA
  //       ref: http://www.bouncycastle.org/wiki/display/JA1/Elliptic+Curve+Key+Pair+Generation+and+Key+Factories
  //            http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
  val ECSpec: String               = "prime256v1"
  val KeyPairAlgorithm: String     = "ECDSA"
  val ProviderName: String         = "BC"
  val SecretKeyFactoryAlgo: String = "desede"
  val CipherAlgo: String           = "desede/CBC/PKCS5Padding"
  val KeyFactoryAlgo: String       = "ECDH"

  object SignAlgo {
    val SHA256withECDSA = "SHA256withECDSA"
  }

  def hash(source: Array[Byte]): Array[Byte] = sha3(source)

  def sha3(source: Array[Byte]): Array[Byte] =
    new SHA3.Digest256().digest(source)

  def makeSignature(source: Array[Byte], priv: PrivateKey, algo: String = SignAlgo.SHA256withECDSA): Array[Byte] = {
    val signInst = Signature.getInstance(algo, ProviderName)
    signInst.initSign(priv)
    signInst.update(source)
    signInst.sign()
  }

  def verifySignature(sign: Array[Byte],
                      source: Array[Byte],
                      publ: PublicKey,
                      algo: String = SignAlgo.SHA256withECDSA): Boolean = {
    val signInst = Signature.getInstance(algo, ProviderName)
    signInst.initVerify(publ)
    signInst.update(source)
    signInst.verify(sign)
  }

  /** create ec keypair
    */
  def generateECKeyPair(): KeyPair = {
    val ecSpec = ECNamedCurveTable.getParameterSpec(ECSpec)
    val g      = KeyPairGenerator.getInstance(KeyPairAlgorithm, ProviderName)
    g.initialize(ecSpec, new SecureRandom())
    g.generateKeyPair()
  }

  def getECPublicKey(kp: KeyPair): Array[Byte] =
    kp.getPublic.asInstanceOf[ECPublicKey].getQ.getEncoded(true)


  def getECPrivateKey(kp: KeyPair): Array[Byte] =
    kp.getPrivate.asInstanceOf[ECPrivateKey].getD.toByteArray

  def rebuildECPublicKey(bytesValue: Array[Byte]): PublicKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val pubSpec = new ECPublicKeySpec(ecSpec.getCurve.decodePoint(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePublic(pubSpec)
  }

  def rebuildECPrivateKey(bytesValue: Array[Byte]): PrivateKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val prvSpec = new ECPrivateKeySpec(new BigInteger(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePrivate(prvSpec)
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

  def des3cbcDecrypt(source: Array[Byte], key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(key)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, desKey, ivSpec)
    cipher.doFinal(source)
  }

}
