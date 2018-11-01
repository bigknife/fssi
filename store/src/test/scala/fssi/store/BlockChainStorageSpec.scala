package fssi.store

import org.scalatest._
import java.io._
import fssi.store.types._

class BlockChainStorageSpec extends FunSuite with BeforeAndAfter {
  var storage: BlockChainStorage = _
  before {
    val root = new File("/tmp/blockchain")
    storage = BlockChainStorage.xodus(root)
  }
  after {
    storage.close()
  }

  test("storage") {
    val key   = StoreKey.meta
    val value = "hello,world".getBytes
    val s     = storage.put(key, value)
    info(s"$s")
    val v1    = storage.get(key)
    assert(v1.isDefined)
    assert(v1.get.bytes sameElements value)

    val height = StoreKey.metaHeight
    val heightValue = BigInt(0)
    storage.put(height, heightValue.toByteArray)

    val v1prime    = storage.get(key).get
    info(s"${v1prime.childrenKeys}")
  }

  /*
  test("transaction success") {
    val k1 = StoreKey.meta
    val v1 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(3.toByte))

    val k2 = StoreKey.block
    val v2 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(2.toByte))

    val k3 = StoreKey.stateAccount("hello")
    val v3 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(2.toByte))

    storage.transact { store =>
      store.put(k1, v1)
      store.put(k2, v2)
      store.put(k3, v3)
      ()
    }

    val v3p = storage.get(k3)
    assert(v3p.isDefined)
    assert(v3p.get === v3)
  }


  test("transaction fail") {
    val k1 = StoreKey.meta
    val v1 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(10.toByte))

    val k2 = StoreKey.block
    val v2 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(20.toByte))

    val k3 = StoreKey.stateAccount("hello")
    val v3 = StoreValue(Array.emptyByteArray, StoreKeySet.empty, Array.fill(10)(50.toByte))

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
   */
}
