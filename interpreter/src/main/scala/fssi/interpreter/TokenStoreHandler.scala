package fssi
package interpreter

import types._, implicits._
import ast._

class TokenStoreHandler extends TokenStore.Handler[Stack] {

}

object TokenStoreHandler {
  private val instance = new TokenStoreHandler()

  trait Implicits {
    implicit val tokenStoreHandlerInstance: TokenStoreHandler = instance
  }
}
