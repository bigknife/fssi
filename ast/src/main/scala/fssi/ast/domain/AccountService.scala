package fssi.ast.domain

import bigknife.sop._, macros._, implicits._
import fssi.ast.domain.types.{Account, BytesValue, KeyPair}

@sp trait AccountService[F[_]] {
  /** create an account with a random string
    *
    * @param publ public key of the account
    * @param priv des3cbc encrypted private key of the account
    * @param iv iv spec for des3cbc
    * @return an account
    */
  def createAccount(publ: BytesValue, priv: BytesValue, iv: BytesValue, uuid: String): P[F, Account]

  /**
    * desensitize the account data
    * @param account account
    * @return an account without sensitive info.
    */
  def desensitize(account: Account): P[F, Account]

  /**
    * make a snapshot for account
    * @param account account
    * @return
    */
  def makeSnapshot(account: Account): P[F, Account.Snapshot]
}
