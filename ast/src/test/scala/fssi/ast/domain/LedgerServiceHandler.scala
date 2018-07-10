package fssi.ast.domain

import cats.Id

class LedgerServiceHandler extends LedgerService.Handler[Id]{

}

object LedgerServiceHandler {
  trait Implicits {
    implicit val ledgerServiceHandler: LedgerServiceHandler = new LedgerServiceHandler
  }
}
