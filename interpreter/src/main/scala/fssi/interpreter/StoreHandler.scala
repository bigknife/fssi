package fssi
package interpreter
import fssi.ast.Store

class StoreHandler extends Store.Handler[Stack] {}

object StoreHandler {
  val instance = new StoreHandler

  trait Implicits {
    implicit val storeHandler: StoreHandler = instance
  }
}
