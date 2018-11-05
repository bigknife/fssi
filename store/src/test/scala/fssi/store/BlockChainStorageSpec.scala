package fssi.store

import org.scalatest._
import java.io._
import fssi.store.types._

class BlockChainStorageSpec extends FunSuite with BeforeAndAfter {
  var storage: BlockChainStorage = _
  before {
    val root = new File("/tmp/blockchain")
    storage = BlockChainStorage.xodus(root, "fixed")
  }
  after {
    storage.close()
  }

  test("storage") {
    val key   = StoreKey.meta
    val value = "hello,world".getBytes
    val s     = storage.put(key, value)
    s match {
      case Left(e) => e.printStackTrace()
      case _ =>
    }
    info(s"$s")
    s match {
      case Left(t) => t.printStackTrace()
      case _ =>
    }
    val v1    = storage.get(key)
    assert(v1.isDefined)
    assert(v1.get.bytes sameElements value)

    val height = StoreKey.metaHeight
    val heightValue = BigInt(0)
    storage.put(height, heightValue.toByteArray)

    val v1prime    = storage.get(key).get
    info(s"${v1prime.childrenKeys}")
  }


  test("transaction success") {
    val k1 = StoreKey.meta
    val v1 = "hello".getBytes

    val k2 = StoreKey.block
    val v2 = "world".getBytes

    val k3 = StoreKey.stateAccount("hello")
    val v3 = "kitty".getBytes

    storage.transact { store =>
      store.put(k1, v1)
      store.put(k2, v2)
      store.put(k3, v3)
      ()
    }

    val v3p = storage.get(k3)
    assert(v3p.isDefined)
    assert(v3p.get.bytes sameElements  v3)
  }


  test("transaction fail") {
    val k1 = StoreKey.meta
    val v1 = "hello1".getBytes

    val k2 = StoreKey.block
    val v2 = "world1".getBytes

    val k3 = StoreKey.stateAccount("hello")
    val v3 = "kitty1".getBytes

    storage.transact { store =>
      store.put(k1, v1)
      store.put(k2, v2)
      store.put(k3, v3)

      throw new RuntimeException("this is a failing test")
    }

    val v3p = storage.get(k3)
    assert(v3p.isDefined)
    assert(v3p.get !== v3)
  }
}
