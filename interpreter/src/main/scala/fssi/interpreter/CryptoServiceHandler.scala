package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._
import fssi.interpreter.util._

/**
  * Based on EC Algorithm
  */
class CryptoServiceHandler extends CryptoService.Handler[Stack] {

  override def generateKeyPair(): Stack[KeyPair] = Stack {
    crypto.generateKeyPair()
  }

  override def randomChar(len: Int): Stack[Array[Char]] = Stack {
    crypto.randomChar(len)
  }

  override def enforceDes3Key(key: BytesValue): Stack[BytesValue] = Stack {
    crypto.enforceDes3Key(key)
  }

  override def des3cbcEncrypt(source: BytesValue,
                              key: BytesValue,
                              iv: BytesValue): Stack[BytesValue] = Stack {
    crypto.des3cbcEncrypt(source, key, iv)

  }

  override def randomUUID(): Stack[String] = Stack {
    crypto.randomUUID()
  }

  override def validateSignature(sign: Signature,
                                 source: BytesValue,
                                 publ: KeyPair.Publ): Stack[Boolean] = Stack {
    crypto.validateSignature(sign, source, publ)
  }

  override def makeSignature(source: BytesValue, priv: KeyPair.Priv): Stack[BytesValue] = Stack {
    crypto.makeSignature(source, priv)
  }

  override def privateKeyData(priv: KeyPair.Priv): Stack[BytesValue] = Stack {
    crypto.privateKeyData(priv)
  }

  override def publicKeyData(publ: KeyPair.Publ): Stack[BytesValue] = Stack {
    crypto.publicKeyData(publ)
  }

  override def rebuildPubl(bytesValue: BytesValue): Stack[KeyPair.Publ] = Stack {
    crypto.rebuildPubl(bytesValue)
  }

  override def rebuildPriv(bytesValue: BytesValue): Stack[KeyPair.Priv] = Stack {
    crypto.rebuildPriv(bytesValue)
  }

  override def des3cbcDecrypt(source: BytesValue,
                              key: BytesValue,
                              iv: BytesValue): Stack[BytesValue] = Stack {
    crypto.des3cbcDecrypt(source, key, iv)
  }

  override def hash(source: BytesValue): Stack[BytesValue] = Stack {
    crypto.hash(source)
  }
}

object CryptoServiceHandler {
  private val instance = new CryptoServiceHandler
  trait Implicits {
    implicit val cryptoServiceHandler: CryptoServiceHandler = instance
  }
  object implicits extends Implicits
}
