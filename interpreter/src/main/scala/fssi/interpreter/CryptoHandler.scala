package fssi
package interpreter

import types._
import ast._

import java.security._
import javax.crypto._
import javax.crypto.spec._
import org.bouncycastle.jce._

/**
  * CryptoHandler uses ECDSA
  * ECDSA
  *      ref: http://www.bouncycastle.org/wiki/display/JA1/Elliptic+Curve+Key+Pair+Generation+and+Key+Factories
  *           http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
  */
class CryptoHandler extends Crypto.Handler[Stack] {

  // keypair algorithm
  private val KP_ALGO              = "ECDSA"
  private val EC_SPEC              = "prime256v1"
  private val SEC_KEY_FACTORY_ALGO = "desede"
  private val CIPHER_ALGO          = "desede/CBC/PKCS5Padding"

  // register bcprovider
  Security.addProvider(new provider.BouncyCastleProvider())

  override def createKeyPair(): Stack[(BytesValue, BytesValue)] = Stack { setting =>
    val ecSpec = ECNamedCurveTable.getParameterSpec(EC_SPEC)
    val g      = KeyPairGenerator.getInstance(KP_ALGO, "BC")
    g.initialize(ecSpec, new SecureRandom())
    val kp = g.generateKeyPair()
    (BytesValue(kp.getPublic.getEncoded), BytesValue(kp.getPrivate.getEncoded))
  }

  override def createIVForDes(): Stack[BytesValue] = Stack { setting =>
    //initialization vector is needs 8 random bytes
    //and, for the reason of readability, they should be readable ascii characters.
    @scala.annotation.tailrec
    def loop(i: Int, acc: Vector[Char]): Vector[Char] =
      if (i == 0) acc
      else {
        val newChar: Char = (scala.util.Random.nextInt(26) + 97).toChar
        loop(i - 1, acc :+ newChar)
      }

    BytesValue(loop(8, Vector.empty).map(_.toByte).toArray)
  }

  override def desEncryptPrivateKey(privateKey: BytesValue,
                                    iv: BytesValue,
                                    password: BytesValue): Stack[BytesValue] = Stack { setting =>
    // ensure the password is 24b length
    val ensuredPass = ensure24Bytes(password)

    val spec       = new DESedeKeySpec(ensuredPass.value)
    val keyFactory = SecretKeyFactory.getInstance(SEC_KEY_FACTORY_ALGO)
    val desKey     = keyFactory.generateSecret(spec)
    val cipher     = Cipher.getInstance(CIPHER_ALGO)
    val ivSpec     = new IvParameterSpec(iv.value)
    cipher.init(Cipher.ENCRYPT_MODE, desKey, ivSpec)

    BytesValue(cipher.doFinal(privateKey.value))
  }

  private def ensure24Bytes(x: BytesValue): BytesValue = x match {
    case a if a.value.length == 24 => a
    case a if a.value.length > 24  => BytesValue(a.value.slice(0, 24))
    case a                         => BytesValue(java.nio.ByteBuffer.allocate(24).put(a.value).array)
  }
}

object CryptoHandler {
  private val instance = new CryptoHandler
  trait Implicits {
    implicit val cryptoHandlerInstance: CryptoHandler = instance
  }

  object implicits extends Implicits
}
