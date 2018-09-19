package fssi.utils

import org.bouncycastle.jcajce.provider.digest.SHA3
import java.security._
import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec, SecretKeySpec, PBEKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory, SecretKey, KeyGenerator}
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
  val SECP256K1: String            = "secp256k1"
  val KeyPairAlgorithm: String     = "ECDSA"
  val ProviderName: String         = "BC"
  val SecretKeyFactoryAlgo: String = "desede"
  val CipherAlgo: String           = "desede/CBC/PKCS5Padding"
  val KeyFactoryAlgo: String       = "ECDH"

  object SignAlgo {
    val SHA256withECDSA = "SHA256withECDSA"
  }

  // create aes key
  def createAesSecretKey(seed: Array[Byte]): SecretKey = {
    val sr = new SecureRandom
    val salt = Array.fill(16)(0.toByte)
    sr.nextBytes(salt)

    val spec = new PBEKeySpec(seed.map(_.toChar), salt, 65536, 256) //aes-256
    val sf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    sf.generateSecret(spec)
  }

  // use aes to encrypt
  def aesEncryptPrivKey(ivBytes: Array[Byte], key: Array[Byte], source: Array[Byte]): Array[Byte] = {
    val iv: IvParameterSpec = new IvParameterSpec(ivBytes)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val k = new SecretKeySpec(key, "AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, k, iv)
    cipher.update(source)
    cipher.doFinal
  }

  def hash(source: Array[Byte]): Array[Byte] = sha3(source)

  def sha3(source: Array[Byte]): Array[Byte] =
    new SHA3.Digest256().digest(source)

  def makeSignature(source: Array[Byte],
                    priv: PrivateKey,
                    algo: String = SignAlgo.SHA256withECDSA): Array[Byte] = {
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
  def generateECKeyPair(spec: String = ECSpec): KeyPair = {
    val ecSpec = ECNamedCurveTable.getParameterSpec(spec)
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

  def ensure24Bytes(x: BytesValue): BytesValue = x match {
    case a if a.value.length == 24 => a
    case a if a.value.length > 24  => BytesValue(a.value.slice(0, 24))
    case a                         => BytesValue(java.nio.ByteBuffer.allocate(24).put(a.value).array)
  }

  def randomBytes(length: Int): Array[Byte] = {
    val sr = new SecureRandom
    val bytes = Array.fill[Byte](length)(0)
    sr.nextBytes(bytes)
    bytes
  }

  def ripemd160(source: Array[Byte]): Array[Byte] = {
    import org.bouncycastle.crypto.digests.{RIPEMD160Digest => Hash160}
    val d = new Hash160
    d.update(source, 0, source.length)
    val o = Array.fill[Byte](d.getDigestSize)(0)
    d.doFinal(o, 0)
    o
  }

  def sha256(source: Array[Byte]): Array[Byte] = {
    java.security.MessageDigest.getInstance("SHA-256").digest(source)
  }

}
