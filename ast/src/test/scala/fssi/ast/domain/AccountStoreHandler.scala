package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.Account

trait AccountStoreHandler extends AccountStore.Handler[Id]{
}

object AccountStoreHandler {
  trait Implicits {
    implicit val accountStore: AccountStoreHandler = new AccountStoreHandler {}
  }
  object implicits extends Implicits
}
