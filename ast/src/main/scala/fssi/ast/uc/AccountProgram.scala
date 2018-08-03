package fssi
package ast
package uc

import types._
import types.syntax._
import bigknife.sop._
import bigknife.sop.implicits._

trait AccountProgram[F[_]] {
  val model: components.Model[F]
  import model._

  /**
    * Create an account, only a password is needed.
    * NOTE: then password is ensured to be 24Bytes length.
    */
  def createAccount(password: String): SP[F, Account] = {
    for {
      keypair <- crypto.createKeyPair()
      (publicKey, privateKey) = keypair
      iv <- crypto.createIVForDes()
      pk <- crypto.desEncryptPrivateKey(privateKey, iv, password = password.getBytes("utf-8"))
    } yield Account(publicKey.toHexString, pk.toHexString, iv.toHexString)
  }
}

object AccountProgram {
  def apply[F[_]](implicit M: components.Model[F]): AccountProgram[F] = new AccountProgram[F] {
    val model: components.Model[F] = M
  }
}
