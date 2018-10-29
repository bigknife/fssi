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

  def registerBC(): Int =
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

  object EncAlgo {
    val AES = "AES/CBC/PKCS5Padding" //"AES/CBC/PKCS5Padding"
  }

  object SignAlgo {
    val SHA256withECDSA = "SHA256withECDSA"
  }

  // create aes key
  def createAesSecretKey(seed: Array[Byte]): SecretKey = {
    val sr   = new SecureRandom
    val salt = Array.fill(16)(0.toByte)
    sr.nextBytes(salt)

    val spec = new PBEKeySpec(seed.map(_.toChar), salt, 58, 256) //aes-256
    //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
    val sf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", ProviderName)
    sf.generateSecret(spec)
  }

  // use aes to encrypt
  def aesEncryptPrivKey(ivBytes: Array[Byte],
                        key: Array[Byte],
                        source: Array[Byte]): Array[Byte] = {
    val iv: IvParameterSpec = new IvParameterSpec(ivBytes)

    def loop(s: Array[Byte], acc: Array[Byte]): Array[Byte] =
      if (s.isEmpty) acc
      else {
        val block  = s.take(15)
        val cipher = Cipher.getInstance(EncAlgo.AES, ProviderName)
        val k      = new SecretKeySpec(key, EncAlgo.AES)
        cipher.init(Cipher.ENCRYPT_MODE, k, iv)
        cipher.update(block)
        val accNext = acc ++ cipher.doFinal
        loop(s.drop(15), accNext)
      }

    loop(source, Array.emptyByteArray)
  }

  // use aes to decrypt
  def aesDecryptPrivKey(ivBytes: Array[Byte],
                        key: Array[Byte],
                        encryption: Array[Byte]): Array[Byte] = {
    require(encryption.length % 16 == 0, "aes encrypted data should be 16-times length")
    val iv: IvParameterSpec = new IvParameterSpec(ivBytes)

    def loop(s: Array[Byte], acc: Array[Byte]): Array[Byte] =
      if (s.isEmpty) acc
      else {
        val block  = s.take(16)
        val cipher = Cipher.getInstance(EncAlgo.AES, ProviderName)
        val k      = new SecretKeySpec(key, EncAlgo.AES)
        cipher.init(Cipher.DECRYPT_MODE, k, iv)
        cipher.update(block)
        val accNext = acc ++ cipher.doFinal
        loop(s.drop(16), accNext)
      }

    loop(encryption, Array.emptyByteArray)

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

  def rebuildECPublicKey(bytesValue: Array[Byte], spec: String = ECSpec): PublicKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(spec)
    val pubSpec = new ECPublicKeySpec(ecSpec.getCurve.decodePoint(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePublic(pubSpec)
  }

  def rebuildECPrivateKey(bytesValue: Array[Byte], spec: String = ECSpec): PrivateKey = {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(spec)
    val prvSpec = new ECPrivateKeySpec(new BigInteger(bytesValue), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    kf.generatePrivate(prvSpec)
  }

  def createDESSecretKey(seed: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(seed)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo, ProviderName)
    val secretKey  = keyFactory.generateSecret(spec)
    secretKey.getEncoded
  }

  def des3cbcEncrypt(source: Array[Byte], key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(key)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo, ProviderName)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo, ProviderName)
    val ivSpec     = new IvParameterSpec(iv)
    cipher.init(Cipher.ENCRYPT_MODE, desKey, ivSpec)

    cipher.doFinal(source)
  }

  def des3cbcDecrypt(source: Array[Byte], key: Array[Byte], iv: Array[Byte]): Array[Byte] = {
    val spec       = new DESedeKeySpec(key)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo, ProviderName)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo, ProviderName)
    val ivSpec     = new IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, desKey, ivSpec)
    cipher.doFinal(source)
  }

  def ensure24Bytes(x: Array[Byte]): Array[Byte] = x match {
    case a if a.length == 24 => a
    case a if a.length > 24  => a.slice(0, 24)
    case a                   => java.nio.ByteBuffer.allocate(24).put(a).array
  }

  def randomBytes(length: Int): Array[Byte] = {
    val sr    = new SecureRandom
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
    java.security.MessageDigest.getInstance("SHA-256", ProviderName).digest(source)
  }

  implicit lazy val sha_256digestInstance: java.security.MessageDigest =
    java.security.MessageDigest.getInstance("SHA-256", ProviderName)
  implicit lazy val sha3_256digestInstance: java.security.MessageDigest = new SHA3.Digest256
}
