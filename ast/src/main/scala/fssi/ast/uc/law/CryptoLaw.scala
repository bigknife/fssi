package fssi.ast.uc.law

import bigknife.sop.SP
import bigknife.sop.implicits._
import fssi.ast.blockchain
import fssi.ast.uc.BaseProgram

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
      privKey    <- crypto.decryptAccountPrivKey(encPrivKey, sk, iv)
    } yield kp.privKey === privKey
  }
}

object CryptoLaw {

  def apply[F[_]](implicit M: blockchain.Model[F]): CryptoLaw[F] = new CryptoLaw[F] {
    override private[uc] val model = M
  }
}
