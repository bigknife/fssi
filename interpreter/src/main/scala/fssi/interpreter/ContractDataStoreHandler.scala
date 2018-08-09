package fssi
package interpreter

import types._, implicits._
import ast._

class ContractDataStoreHandler extends ContractDataStore.Handler[Stack] {

}

object ContractDataStoreHandler {
  private val instance = new ContractDataStoreHandler()

  trait Implicits {
    implicit val contractDataStoreHandlerInstance: ContractDataStoreHandler = instance
  }
}
