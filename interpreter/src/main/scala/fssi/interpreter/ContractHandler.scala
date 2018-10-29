package fssi
package interpreter
import fssi.ast.Contract

class ContractHandler extends Contract.Handler[Stack] {}

object ContractHandler {
  val instance = new ContractHandler

  trait Implicits {
    implicit val contractHandler: ContractHandler = instance
  }
}
