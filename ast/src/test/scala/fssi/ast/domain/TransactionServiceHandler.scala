package fssi.ast.domain

import cats.Id

trait TransactionServiceHandler extends TransactionService.Handler[Id] {}

object TransactionServiceHandler {
  trait Implicits {
    implicit val transactionServiceHandler: TransactionServiceHandler =
      new TransactionServiceHandler {}
  }

  object implicits extends Implicits
}
