package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class ContractStoreHandler extends ContractStore.Handler[Stack] {}
object ContractStoreHandler {
  trait Implicits {
    implicit val contractStoreHandler: ContractStoreHandler = new ContractStoreHandler
  }
  object implicits extends Implicits
}
