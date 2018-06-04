package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class AccountServiceHandler extends AccountService.Handler[Stack] {
  override def createAccount(publ: BytesValue,
                             priv: BytesValue,
                             iv: BytesValue,
                             uuid: String): Stack[Account] = Stack {
    Account(
      id = Account.ID(uuid),
      privateKeyData = priv,
      publicKeyData = publ,
      iv = iv,
      balance = Token.Zero
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
  trait Implicits {
    implicit val accountServiceHandler: AccountServiceHandler = new AccountServiceHandler
  }
}
