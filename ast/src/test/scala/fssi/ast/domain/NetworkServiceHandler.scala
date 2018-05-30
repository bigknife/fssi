package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.DataPacket

trait NetworkServiceHandler extends NetworkService.Handler[Id]{
}
object NetworkServiceHandler {
  trait Implicits {
    implicit val networkServiceHandler: NetworkServiceHandler = new NetworkServiceHandler {}
  }
  object implicits extends Implicits
}
