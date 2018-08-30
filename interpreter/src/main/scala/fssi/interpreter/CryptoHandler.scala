package fssi
package interpreter

import types._, exception._
import utils._
import ast._

import scala.util._

/**
  * CryptoHandler uses ECDSA
  * ECDSA
  *      ref: http://www.bouncycastle.org/wiki/display/JA1/Elliptic+Curve+Key+Pair+Generation+and+Key+Factories
  *           http://www.bouncycastle.org/wiki/pages/viewpage.action?pageId=362269
  */
class CryptoHandler extends Crypto.Handler[Stack] with LogSupport {

  override def createKeyPair(): Stack[(BytesValue, BytesValue)] = Stack { setting =>
    val kp = crypto.generateECKeyPair()
    (BytesValue(crypto.getECPublicKey(kp)), BytesValue(crypto.getECPrivateKey(kp)))
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
    val ensuredPass = crypto.ensure24Bytes(password)
    BytesValue(crypto.des3cbcEncrypt(privateKey.value, ensuredPass.value, iv.value))
  }

  override def desDecryptPrivateKey(
      encryptedPrivateKey: BytesValue,
      iv: BytesValue,
      password: BytesValue): Stack[Either[FSSIException, BytesValue]] = Stack { setting =>
    Try {
      val ensuredPass = crypto.ensure24Bytes(password)
      BytesValue(crypto.des3cbcDecrypt(encryptedPrivateKey.value, ensuredPass.value, iv.value))
    }.toEither.left.map(x => new FSSIException("decrypt private key faield", Some(x)))
  }

  override def makeSignature(source: BytesValue, privateKey: BytesValue): Stack[Signature] = Stack {
    setting =>
      Signature(
        HexString(
          crypto.makeSignature(
            source = source.value,
            priv = crypto.rebuildECPrivateKey(privateKey.value)
          )
        ))
  }

  /** verify signature
    */
  override def verifySignature(source: BytesValue,
                               publicKey: BytesValue,
                               signature: Signature): Stack[Boolean] = Stack { setting =>
    scala.util.Try {
      crypto.verifySignature(
        sign = signature.value.bytes,
        source = source.value,
        publ = crypto.rebuildECPublicKey(publicKey.value)
      )
    }.toEither match {
      case Left(t) =>
        log.error("verify signature faield", t)
        false
      case Right(x) => x
    }
  }
}

object CryptoHandler {
  val instance = new CryptoHandler
  trait Implicits {
    implicit val cryptoHandlerInstance: CryptoHandler = instance
  }

  object implicits extends Implicits
}
