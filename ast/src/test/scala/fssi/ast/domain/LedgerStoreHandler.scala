package fssi.ast.domain

import cats.Id

class LedgerStoreHandler extends LedgerStore.Handler[Id] {}

object LedgerStoreHandler {
  trait Implicits {
    implicit val ledgerStoreHandler = new LedgerStoreHandler
  }

  object implicits extends Implicits
}
