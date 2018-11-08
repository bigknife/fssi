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

    bcs.putMeta(1, MetaKey.ChainID, MetaData("testnet1".getBytes("utf-8")))
    bcs.putMeta(1, MetaKey.Height, MetaData(BigInt(1).toByteArray))
    bcs.putMeta(1, MetaKey.Version, MetaData("1.0".getBytes("utf-8")))

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
    bcs.putMeta(1, MetaKey.ChainID, MetaData("testnet1".getBytes("utf-8")))
    bcs.putMeta(1, MetaKey.Height, MetaData(BigInt(1).toByteArray))
    bcs.putMeta(1, MetaKey.Version, MetaData("1.0".getBytes("utf-8")))

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
}
