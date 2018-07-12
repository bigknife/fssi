package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class AccountServiceHandler extends AccountService.Handler[Stack] {
  override def createAccount(publ: BytesValue,
                             priv: BytesValue,
                             iv: BytesValue,
                             uuid: String): Stack[Account] = Stack {
    Account(
      privateKeyData = priv,
      publicKeyData = publ,
      iv = iv,
      balance = Token(10000, Token.Unit.Sweet) //todo: remove, for test, init amount is 10000
    )
  }


  override def desensitize(account: Account): Stack[Account] = Stack {
    account.copy(privateKeyData = BytesValue.Empty)
  }

  override def makeSnapshot(account: Account): Stack[Account.Snapshot] = Stack {
    Account.Snapshot(System.currentTimeMillis(), account, Account.Snapshot.Created)
  }
}

object AccountServiceHandler {
  private val instance = new AccountServiceHandler
  trait Implicits {
    implicit val accountServiceHandler: AccountServiceHandler = instance
  }
}
