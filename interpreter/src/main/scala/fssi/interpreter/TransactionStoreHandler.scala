package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class TransactionStoreHandler extends TransactionStore.Handler[Stack] {}

object TransactionStoreHandler {
  trait Implicits {
    implicit val transactionStoreHandler: TransactionStoreHandler = new TransactionStoreHandler
  }
  object implicits extends Implicits
}
