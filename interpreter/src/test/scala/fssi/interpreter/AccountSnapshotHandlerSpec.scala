package fssi.interpreter

import fssi.ast.domain.types._
import fssi.interpreter.util.SnapshotDB
import org.scalatest._

class AccountSnapshotHandlerSpec extends FunSuite with BeforeAndAfterAll {
  val accountSnapshotHandler = new AccountSnapshotHandler
  val setting: Setting = Setting()

  override protected def afterAll(): Unit = {
    accountSnapshotHandler.shutdownSnapshotDB()(setting).unsafeRunSync()
  }

  override protected def beforeAll(): Unit = {
    accountSnapshotHandler.startupSnapshotDB()(setting).unsafeRunSync()
  }

  test("saveSnapshot") {
    val accountId = Account.ID("1")
    val account = Account(
      privateKeyData = BytesValue.Empty,
      publicKeyData = BytesValue("fake publick key"),
      iv = BytesValue("fake iv"),
      balance = Token.Zero
    )
    val snapshot1 = Account.Snapshot(1, account, Account.Snapshot.Created)
    val snapshot2 = Account.Snapshot(2, account, Account.Snapshot.Created)

    // save snapshot1
    val s1 = accountSnapshotHandler.saveSnapshot(snapshot1)(setting).unsafeRunSync()
    assert(s1 == snapshot1)


    val s1Opt = accountSnapshotHandler.findAccountSnapshot(accountId)(setting).unsafeRunSync()
    assert(s1Opt.isDefined)
    assert(s1Opt.contains(s1))

    // update snapshot
    val s2 = accountSnapshotHandler.saveSnapshot(snapshot2)(setting).unsafeRunSync()
    val s2Opt = accountSnapshotHandler.findAccountSnapshot(accountId)(setting).unsafeRunSync()
    assert(s2Opt.isDefined)
    assert(s2Opt.get.timestamp == 2)
  }

}
