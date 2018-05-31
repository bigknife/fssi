package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class NetworkServiceHandler extends NetworkService.Handler[Stack] {}
object NetworkServiceHandler {
  trait Implicits {
    implicit val networkServiceHandler: NetworkServiceHandler = new NetworkServiceHandler
  }
  object implicits extends Implicits
}
