package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class AccountServiceHandler extends AccountService.Handler[Stack] {

}

object AccountServiceHandler {
  trait Implicits {
    implicit val accountServiceHandler: AccountServiceHandler = new AccountServiceHandler
  }
}
