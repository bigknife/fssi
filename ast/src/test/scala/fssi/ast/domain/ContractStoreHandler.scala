package fssi.ast.domain

import cats.Id

class ContractStoreHandler extends ContractStore.Handler[Id] {}
object ContractStoreHandler {
  trait Implicits {
    implicit val contractStoreHandler: ContractStoreHandler = new ContractStoreHandler
  }
  object implicits extends Implicits
}
