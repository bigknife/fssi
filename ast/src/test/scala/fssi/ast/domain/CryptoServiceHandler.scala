package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.{BytesValue, KeyPair}

trait CryptoServiceHandler extends CryptoService.Handler[Id]{
  override def generateKeyPair(): Id[KeyPair] = super.generateKeyPair()

  override def des3cbcEncrypt(source: BytesValue, key: BytesValue, iv: BytesValue): Id[BytesValue] = super.des3cbcEncrypt(source, key, iv)

  override def enforceDes3Key(key: BytesValue): Id[BytesValue] = super.enforceDes3Key(key)

  override def randomChar(len: Int): Id[Array[Char]] = super.randomChar(len)

  override def randomUUID(): Id[String] = super.randomUUID()
}

object CryptoServiceHandler {
  trait Implicits {
    implicit  val cryptoServiceHandler = new CryptoServiceHandler {}
  }
  object implicits extends Implicits
}