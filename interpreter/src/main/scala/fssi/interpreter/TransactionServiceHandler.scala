package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class TransactionServiceHandler extends TransactionService.Handler[Stack] {}

object TransactionServiceHandler {
  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler =
      new TransactionServiceHandler
  }

  object implicits extends Implicits
}
