package fssi.store.bcs

import fssi.store.bcs.types._
import fssi.store.bcs.types.BCSKey._
import org.scalatest.{BeforeAndAfter, FunSuite}
import java.io._

class BCSSpec extends FunSuite with BeforeAndAfter {

  var bcs: BCS = _

  before {
    bcs = BCS("/tmp/mybcs")
  }
  after {
    bcs.close()

    def delete(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach { f1 =>
          delete(f1)
        }
      }
      if (!f.delete()) throw new FileNotFoundException(s"Failed to delete file: $f")
    }
    delete(new File("/tmp/mybcs"))
  }

  test("put and commit") {

    bcs.putMeta(1, MetaKey.ChainID, BlockData("testnet1".getBytes("utf-8")))
    bcs.putMeta(1, MetaKey.Height, BlockData(BigInt(1).toByteArray))
    bcs.putMeta(1, MetaKey.Version, BlockData("1.0".getBytes("utf-8")))

    val chainIdSnapshot = bcs.getSnapshotMeta(MetaKey.ChainID)
    val heightSnapshot  = bcs.getSnapshotMeta(MetaKey.Height)
    val versionSnapshot = bcs.getSnapshotMeta(MetaKey.Version)

    val chainIdPersisted = bcs.getPersistedMeta(MetaKey.ChainID)
    val heightPersisted  = bcs.getPersistedMeta(MetaKey.Height)
    val versionPersisted = bcs.getPersistedMeta(MetaKey.Version)

    info(s"chainIdS = $chainIdSnapshot")
    info(s"heightS = $heightSnapshot")
    info(s"versionS = $versionSnapshot")

    info(s"chainIdP = $chainIdPersisted")
    info(s"heightP = $heightPersisted")
    info(s"versionP = $versionPersisted")

    bcs.commit(1)
    val chainIdAfterCommit = bcs.getPersistedMeta(MetaKey.ChainID)
    val versionAfterCommit = bcs.getPersistedMeta(MetaKey.Version)
    val heightAfterCommit  = bcs.getPersistedMeta(MetaKey.Height)

    info("====================================")
    assert(chainIdAfterCommit.isRight)
    assert(chainIdAfterCommit.right.get.isDefined)
    info(s"chainIdP = ${new String(chainIdAfterCommit.right.get.get.bytes)}")

    assert(versionAfterCommit.isRight)
    assert(versionAfterCommit.right.get.isDefined)
    info(s"versionP = ${new String(versionAfterCommit.right.get.get.bytes)}")

    assert(heightAfterCommit.isRight)
    assert(heightAfterCommit.right.get.isDefined)
    info(s"heightP = ${BigInt(heightAfterCommit.right.get.get.bytes)}")
  }

  test("put and rollback") {
    bcs.putMeta(1, MetaKey.ChainID, BlockData("testnet1".getBytes("utf-8")))
    bcs.putMeta(1, MetaKey.Height, BlockData(BigInt(1).toByteArray))
    bcs.putMeta(1, MetaKey.Version, BlockData("1.0".getBytes("utf-8")))

    val chainIdSnapshot = bcs.getSnapshotMeta(MetaKey.ChainID)
    val heightSnapshot  = bcs.getSnapshotMeta(MetaKey.Height)
    val versionSnapshot = bcs.getSnapshotMeta(MetaKey.Version)

    val chainIdPersisted = bcs.getPersistedMeta(MetaKey.ChainID)
    val heightPersisted  = bcs.getPersistedMeta(MetaKey.Height)
    val versionPersisted = bcs.getPersistedMeta(MetaKey.Version)

    info(s"chainIdS = $chainIdSnapshot")
    info(s"heightS = $heightSnapshot")
    info(s"versionS = $versionSnapshot")

    info(s"chainIdP = $chainIdPersisted")
    info(s"heightP = $heightPersisted")
    info(s"versionP = $versionPersisted")

    bcs.rollback(1)
    info("========== AFTER ROLLBACK: PERSISTED AND SNAPSHOT BOTH DELETED ======================")

    {
      val chainIdAfterRollback = bcs.getPersistedMeta(MetaKey.ChainID)
      val versionAfterRollback = bcs.getPersistedMeta(MetaKey.Version)
      val heightAfterRollback  = bcs.getPersistedMeta(MetaKey.Height)

      assert(chainIdAfterRollback.isRight)
      assert(chainIdAfterRollback.right.get.isEmpty)

      assert(versionAfterRollback.isRight)
      assert(versionAfterRollback.right.get.isEmpty)

      assert(heightAfterRollback.isRight)
      assert(heightAfterRollback.right.get.isEmpty)

    }

    {
      val chainIdAfterRollback = bcs.getSnapshotMeta(MetaKey.ChainID)
      val versionAfterRollback = bcs.getSnapshotMeta(MetaKey.Version)
      val heightAfterRollback  = bcs.getSnapshotMeta(MetaKey.Height)

      assert(chainIdAfterRollback.isRight)
      assert(chainIdAfterRollback.right.get.isEmpty)

      assert(versionAfterRollback.isRight)
      assert(versionAfterRollback.right.get.isEmpty)

      assert(heightAfterRollback.isRight)
      assert(heightAfterRollback.right.get.isEmpty)
    }
  }

  test("snapshot transact") {
    // update accountId1.balance + 100 and accountId2.balance - 100
    val accountId1 = "accountId1"
    val accountId2 = "accountId2"

    val b10 = bcs.getSnapshotState(StateKey.balance(accountId1)).right.get
    val b20 = bcs.getSnapshotState(StateKey.balance(accountId2)).right.get

    assert(b10.isEmpty)
    assert(b20.isEmpty)

    bcs.snapshotTransact {proxy =>
      val b1 = proxy.getBalance(accountId1)
      val b2 = proxy.getBalance(accountId2)

      proxy.putBalance(accountId1, b1 + 100)
      proxy.putBalance(accountId2, b2 - 100)
    }

    val b11 = bcs.getSnapshotState(StateKey.balance(accountId1)).right.get
    val b21 = bcs.getSnapshotState(StateKey.balance(accountId2)).right.get

    assert(b11.isDefined)
    assert(b21.isDefined)

    info(s"accountId1 balance in snapshot now is ${BigInt(b11.get.bytes)}")
    info(s"accountId2 balance in snapshot now is ${BigInt(b21.get.bytes)}")

    // persisted area is old, cause not committed now
    val b12 = bcs.getPersistedState(StateKey.balance(accountId1)).right.get
    val b22 = bcs.getPersistedState(StateKey.balance(accountId2)).right.get

    assert(b12.isEmpty)
    assert(b22.isEmpty)
  }
}
