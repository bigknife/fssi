package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

/**
  * Account Snapshot is a store, which save some snapshot info of account.
  * of course, no sensitive info should be stored. so check out that it was desensitized.
  */
class AccountSnapshotHandler extends AccountSnapshot.Handler[Stack]{



}

object AccountSnapshotHandler {
  trait Implicits {
    implicit val accountSnapshot: AccountSnapshotHandler = new AccountSnapshotHandler {}
  }
  object implicits extends Implicits
}
