package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class TransactionStoreHandler extends TransactionStore.Handler[Stack] {}

object TransactionStoreHandler {
  private val instance = new TransactionStoreHandler
  trait Implicits {
    implicit val transactionStoreHandler: TransactionStoreHandler = instance
  }
  object implicits extends Implicits
}
