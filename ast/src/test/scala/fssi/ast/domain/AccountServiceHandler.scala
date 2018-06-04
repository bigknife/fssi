package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.{Account, BytesValue, KeyPair, Token}

class AccountServiceHandler extends AccountService.Handler[Id] {}

object AccountServiceHandler {
  trait Implicits {
    implicit val accountServiceHandler: AccountServiceHandler = new AccountServiceHandler
  }

  object implicits extends Implicits
}
