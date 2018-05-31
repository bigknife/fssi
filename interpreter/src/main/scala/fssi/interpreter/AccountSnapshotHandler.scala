package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

trait AccountSnapshotHandler extends AccountSnapshot.Handler[Stack]{
}

object AccountSnapshotHandler {
  trait Implicits {
    implicit val accountSnapshot: AccountSnapshotHandler = new AccountSnapshotHandler {}
  }
  object implicits extends Implicits
}
