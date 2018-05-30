package fssi.ast.domain

import cats.Id
import fssi.ast.domain.types.Account

trait AccountSnapshotHandler extends AccountSnapshot.Handler[Id]{
}

object AccountSnapshotHandler {
  trait Implicits {
    implicit val accountSnapshot: AccountSnapshotHandler = new AccountSnapshotHandler {}
  }
  object implicits extends Implicits
}
