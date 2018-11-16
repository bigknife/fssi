package fssi.store.core

import jetbrains.exodus.env.{Environment, Environments}
import org.scalatest.FunSuite

class XodusSpec extends FunSuite {
  test("put and get") {
    val environment: Environment = Environments.newInstance("/tmp/testmpt")
    val store: KVStore = new XodusKVStore("test", environment, None)
    assert(store.get("hello".getBytes()).isEmpty)

    store.put("hello".getBytes(), "world".getBytes())
    assert(store.get("hello".getBytes()).isDefined)
    assert(store.get("hello".getBytes()).get sameElements "world".getBytes())
  }
}
