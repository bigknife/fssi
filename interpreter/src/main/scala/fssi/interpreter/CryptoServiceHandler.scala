package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class CryptoServiceHandler extends CryptoService.Handler[Stack] {}

object CryptoServiceHandler {
  trait Implicits {
    implicit val cryptoServiceHandler: CryptoServiceHandler = new CryptoServiceHandler
  }
  object implicits extends Implicits
}
