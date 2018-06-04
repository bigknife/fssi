package fssi.ast.domain

import cats.Id

class NetworkStoreHandler extends NetworkStore.Handler[Id]{

}

object NetworkStoreHandler {
  trait Implicits {
    implicit val networkStoreHandler: NetworkStoreHandler = new NetworkStoreHandler
  }

  object implicits extends Implicits
}
