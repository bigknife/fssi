package fssi.store.mpt

import fssi.store.core.{KVStore, XodusKVStore}
import jetbrains.exodus.env.{Environment, Environments}
import org.scalatest._

import scala.util.Random


class MPTSpec extends FunSuite with BeforeAndAfter {
  import MPTSpec._
  var mpt: MPT = _
  before {
    mpt = MPT("state")
  }

  test("put and get") {
    val times = 10
    for (_ <- 1 to times) {
      val key = Array.fill(32)(0.toByte)
      Random.nextBytes(key)

      val value = Array.fill(1024)(0.toByte)
      Random.nextBytes(value)

      mpt.put(key, value)

      val value1 = mpt.get(key).right.get
      assert(value1.isDefined)
      assert(value1.get sameElements value)
    }
  }


  test("get root: 702183868beb89b05655c15d1a0355d3b316dda02f608f189913e730e747f7ec") {
    /*
    val key = "702183868beb89b05655c15d1a0355d3b316dda02f608f189913e730e747f7ec".getBytes("utf-8")
    val s = mpt.store.get(key).map(Data.wrap)
    info(s"${s.map(_.toNode)}")
    */
    mpt.rootHash.map(Key.encode).foreach(k =>
    info(s"$k")
    )
  }

  test("transact") {
    val k1: Array[Byte] = Array(0, 0, 0, 0, 0, 0, 0, 1)
    val k2: Array[Byte] = Array(0, 0, 0, 0, 0, 0, 0, 2)
    val v: Array[Byte] = Array(1,1,1,1)

    val t =  mpt.transact { proxy =>
      //proxy.put(k1, v)
      proxy.put(k2, v)
    }
    assert(t.isRight)
    val v1 = mpt.get(k2)
    assert(v1.isRight && v1.right.get.isDefined && v1.right.get.get.sameElements(v))

  }

}

object MPTSpec {
  val environment: Environment = Environments.newInstance("/tmp/testmpt")
  implicit val store: KVStore = new XodusKVStore("test", environment, None)
}
