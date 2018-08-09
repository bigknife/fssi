package fssi
package interpreter

import types._, implicits._
import ast._

class ContractStoreHandler extends ContractStore.Handler[Stack] {

}

object ContractStoreHandler {
  private val instance = new ContractStoreHandler()

  trait Implicits {
    implicit val contractStoreHandlerInstance: ContractStoreHandler = instance
  }
}
