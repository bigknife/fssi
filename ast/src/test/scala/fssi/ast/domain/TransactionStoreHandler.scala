package fssi.ast.domain

import cats.Id

class TransactionStoreHandler extends TransactionStore.Handler[Id] {}

object TransactionStoreHandler {
  trait Implicits {
    implicit val transactionStoreHandler: TransactionStoreHandler = new TransactionStoreHandler
  }
  object implicits extends Implicits
}
