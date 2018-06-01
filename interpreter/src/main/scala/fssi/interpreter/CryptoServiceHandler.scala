package fssi.interpreter

import java.math.BigInteger
import java.nio.ByteBuffer

import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}
import java.security.{KeyFactory, KeyPairGenerator, SecureRandom, Security, Signature => JSS}
import java.util.UUID

import fssi.ast.domain._
import fssi.ast.domain.types._
import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{DESedeKeySpec, IvParameterSpec}
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECPrivateKeySpec, ECPublicKeySpec}

/**
  * Based on EC Algorithm
  */
class CryptoServiceHandler extends CryptoService.Handler[Stack] {

  // init bcprovider
  Security.addProvider(new BouncyCastleProvider)

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

  override def generateKeyPair(): Stack[KeyPair] = Stack {
    val ecSpec = ECNamedCurveTable.getParameterSpec(ECSpec)
    val g      = KeyPairGenerator.getInstance(KeyPairAlgorithm, ProviderName)
    g.initialize(ecSpec, new SecureRandom())
    KeyPair.fromJCEKeyPair(g.generateKeyPair())
  }

  override def randomChar(len: Int): Stack[Array[Char]] = Stack {
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

  override def enforceDes3Key(key: BytesValue): Stack[BytesValue] = Stack {
    key match {
      case x if x.bytes.length == 24 => x
      case x if x.bytes.length < 24  => BytesValue(ByteBuffer.allocate(24).put(x.bytes).array())
      case x                         => BytesValue(ByteBuffer.allocate(24).put(x.bytes, 24, 0).array())
    }
  }

  override def des3cbcEncrypt(source: BytesValue,
                              key: BytesValue,
                              iv: BytesValue): Stack[BytesValue] = Stack {
    val spec       = new DESedeKeySpec(key.bytes)
    val keyFactory = SecretKeyFactory.getInstance(SecretKeyFactoryAlgo)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CipherAlgo)
    val ivSpec     = new IvParameterSpec(iv.bytes)
    cipher.init(Cipher.ENCRYPT_MODE, desKey, ivSpec)

    BytesValue(cipher.doFinal(source.bytes))

  }

  override def randomUUID(): Stack[String] = Stack {
    UUID.randomUUID().toString.replaceAll("-", "").toUpperCase
  }

  override def validateSignature(sign: Signature,
                                 source: BytesValue,
                                 publ: KeyPair.Publ): Stack[Boolean] = Stack {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initVerify(publ.publicKey)
    s.update(source.bytes)
    s.verify(sign.bytes)
  }

  override def makeSignature(source: BytesValue, priv: KeyPair.Priv): Stack[BytesValue] = Stack {
    val s = JSS.getInstance(SignAlgo, ProviderName)
    s.initSign(priv.privateKey)
    s.update(source.bytes)
    BytesValue(s.sign())
  }

  override def privateKeyData(priv: KeyPair.Priv): Stack[BytesValue] = Stack {
    BytesValue(priv.privateKey.asInstanceOf[ECPrivateKey].getD.toByteArray)
  }

  override def publicKeyData(publ: KeyPair.Publ): Stack[BytesValue] = Stack {
    BytesValue(publ.publicKey.asInstanceOf[ECPublicKey].getQ.getEncoded(true))
  }

  override def rebuildPubl(bytesValue: BytesValue): Stack[KeyPair.Publ] = Stack {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val pubSpec = new ECPublicKeySpec(ecSpec.getCurve.decodePoint(bytesValue.bytes), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    KeyPair.Publ(kf.generatePublic(pubSpec))
  }

  override def rebuildPriv(bytesValue: BytesValue): Stack[KeyPair.Priv] = Stack {
    val ecSpec  = ECNamedCurveTable.getParameterSpec(ECSpec)
    val prvSpec = new ECPrivateKeySpec(new BigInteger(bytesValue.bytes), ecSpec)
    val kf      = KeyFactory.getInstance(KeyFactoryAlgo, ProviderName)
    KeyPair.Priv(kf.generatePrivate(prvSpec))
  }
}

object CryptoServiceHandler {
  trait Implicits {
    implicit val cryptoServiceHandler: CryptoServiceHandler = new CryptoServiceHandler
  }
  object implicits extends Implicits
}
