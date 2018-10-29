package fssi.ast.uc

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.biz._
import fssi.types.base._
import fssi.ast.uc._

trait CryptoLaw[F[_]] extends BaseProgram[F] {
  import model._

  /** encrypt/decrypt
    */
  def privKeyEncryptAndDecrypt(): SP[F, Boolean] = {
    for {
      kp         <- crypto.createAccountKeyPair()
      rnd        <- crypto.createRandomSeed()
      sk         <- crypto.createSecretKey(rnd)
      iv         <- crypto.createAccountIV()
      encPrivKey <- crypto.encryptAccountPrivKey(kp.privKey, sk, iv)
      privKey    <- crypto.decryptAccountPrivKey(kp.privKey, sk, iv)
    } yield encPrivKey === privKey
  }
}
