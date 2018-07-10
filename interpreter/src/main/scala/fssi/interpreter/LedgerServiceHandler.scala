package fssi.interpreter

import fssi.ast.domain.LedgerService


class LedgerServiceHandler extends LedgerService.Handler[Stack] {

}

object LedgerServiceHandler {
  private val _instance: LedgerServiceHandler = new LedgerServiceHandler

  trait Implicits {
    implicit val ledgerServiceHandler: LedgerServiceHandler = _instance
  }

}
