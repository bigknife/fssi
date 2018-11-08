package fssi.store.bcs

import fssi.store.bcs.types.BCSKey.MetaKey
import fssi.store.bcs.types.{BCSKey, MetaData, _}
import org.scalatest.{BeforeAndAfter, FunSuite}

class BCSSpec extends FunSuite with BeforeAndAfter {

  var bcs: BCS = _

  before {
    bcs = BCS("/tmp/mybcs")
  }
  after {}

  test("getMeta") {

    bcs.putMeta(1, MetaKey.ChainID, MetaData("testnet1".getBytes("utf-8")))

    val chainIdSnapshot = bcs.getSnapshotMeta(MetaKey.ChainID)
    val heightSnapshot  = bcs.getSnapshotMeta(MetaKey.Height)
    val versionSnapshot = bcs.getSnapshotMeta(MetaKey.Version)

    val chainIdPersisted = bcs.getPersistedMeta(MetaKey.ChainID)
    val heightPersisted = bcs.getPersistedMeta(MetaKey.Height)
    val versionPersisted = bcs.getPersistedMeta(MetaKey.Version)

    info(s"chainIdS = $chainIdSnapshot")
    info(s"heightS = $heightSnapshot")
    info(s"versionS = $versionSnapshot")

    info(s"chainIdP = $chainIdPersisted")
    info(s"heightP = $heightPersisted")
    info(s"versionP = $versionPersisted")

    bcs.commit(1)
    val chainIdAfterCommit = bcs.getPersistedMeta(MetaKey.ChainID)
    info("====================================")
    info(s"chainIdS = $chainIdAfterCommit")
  }
}
