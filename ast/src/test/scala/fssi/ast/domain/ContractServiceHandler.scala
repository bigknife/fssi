package fssi.ast.domain

import cats.Id

class ContractServiceHandler extends ContractService.Handler[Id] {}
object ContractServiceHandler {
  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = new ContractServiceHandler
  }
  object implicits extends Implicits
}
