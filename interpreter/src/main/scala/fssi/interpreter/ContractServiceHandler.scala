package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class ContractServiceHandler extends ContractService.Handler[Stack] {}
object ContractServiceHandler {
  trait Implicits {
    implicit val contractServiceHandler: ContractServiceHandler = new ContractServiceHandler
  }
  object implicits extends Implicits
}
