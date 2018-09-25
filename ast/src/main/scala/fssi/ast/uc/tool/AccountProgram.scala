package fssi
package ast
package uc
package tool

import fssi.types.biz._
import fssi.types.base._

import bigknife.sop._
import bigknife.sop.implicits._
import types.implicits._

trait AccountProgram[F[_]] extends BaseProgram[F] {

  import model._

  /** create an account that is compatible with btc
    */
  def createAccount(seed: RandomSeed): SP[F, (Account, Account.SecretKey)] = {
    import accountService._
    for {
      kp <- createSecp256k1KeyPair()
      (pubKey, privKey) = kp
      sk          <- createAesSecretKey(seed)
      iv          <- createAesIV()
      encPrivKey  <- aesEncryptPrivKey(privKey, sk, iv)
      payload     <- doubleHash(pubKey)
      base58check <- base58checkWrapperForAccountId(payload)
    } yield (Account(encPrivKey, pubKey, iv, Account.ID(base58check.asBytesValue.bytes)), sk)
  }
}

object AccountProgram {
  def apply[F[_]](implicit M: components.Model[F]): AccountProgram[F] = new AccountProgram[F] {
    val model: components.Model[F] = M
  }
}
