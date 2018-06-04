package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class LedgerStoreHandler extends LedgerStore.Handler[Stack] {}

object LedgerStoreHandler {
  trait Implicits {
    implicit val ledgerStoreHandler: LedgerStoreHandler = new LedgerStoreHandler
  }

  object implicits extends Implicits
}
