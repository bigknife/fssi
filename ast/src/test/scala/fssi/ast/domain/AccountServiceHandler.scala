package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.{Account, BytesValue, KeyPair, Token}

trait AccountServiceHandler extends AccountService.Handler[Id] {
  override def createAccount(publ: BytesValue,
                             priv: BytesValue,
                             iv: BytesValue,
                             uuid: String): Id[Account] =
    Account(
      Account.ID(uuid),
      KeyPair.Priv(priv.bytes),
      KeyPair.Publ(publ.bytes),
      iv,
      Token.Zero
    )
}

object AccountServiceHandler {
  trait Implicits {
    implicit val accountServiceHandler: AccountServiceHandler = new AccountServiceHandler {}
  }

  object implicits extends Implicits
}
