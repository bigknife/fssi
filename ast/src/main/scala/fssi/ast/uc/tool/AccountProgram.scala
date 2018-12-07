package fssi.ast.uc
package tool

import bigknife.sop._
import bigknife.sop.macros._
import bigknife.sop.implicits._

import fssi.types.base._
import fssi.types.biz._

trait AccountProgram[F[_]] extends ToolProgram[F] with BaseProgram[F] {
  import model._

  /** create an account that is compatible with btc
    */
  def createAccount(seed: RandomSeed): SP[F, (Account, Account.SecretKey)] = {
    for {
      kp         <- crypto.createAccountKeyPair()
      sk         <- crypto.createSecretKey(seed)
      iv         <- crypto.createAccountIV()
      encPrivKey <- crypto.encryptAccountPrivKey(kp.privKey, sk, iv)
      id         <- crypto.createAccountID(kp.pubKey)
      account = Account(encPrivKey, kp.pubKey, iv, id)
    } yield (account, sk)
  }
}
