package fssi.store.mpt

import java.io.{File, FileNotFoundException}

import fssi.store.core.{KVStore, RouteXodusKVStore, XodusKVStore}
import jetbrains.exodus.env.{Environment, Environments}
import org.scalatest._

import scala.util.Random

class MPTSpec extends FunSuite with BeforeAndAfter {

  var rootFile: String = _
  var environment: Environment = _
  var mpt: MPT = _

  before {    rootFile = {
      val bytes = Array.fill(32)(0.toByte)
      scala.util.Random.nextBytes(bytes)
      bytes.map("%02x" format _).mkString("")
    }
    val rootPath = s"/tmp/$rootFile"
    new File(rootPath).mkdirs()
    info(s"current working dir is $rootPath")
    environment = Environments.newInstance(rootPath)
    implicit val store: KVStore  = new RouteXodusKVStore(environment, None) {
      override def routeStoreName(key: Array[Byte]): String = new String(key.take(5))
      override def routeStoreKey(key: Array[Byte]): Array[Byte] = key.drop(5)
    }
    mpt = MPT(keys => "state")
  }
  after {
    environment.close()
    val rootPath = s"/tmp/$rootFile"
    def delete(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach {f1 =>
          delete(f1)
        }
      }
      if (!f.delete()) throw new FileNotFoundException(s"Failed to delete file: $f")
    }
    delete(new File(rootPath))
  }

  test("put and get") {
    val times = 100
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

  test("root key") {
    mpt.rootKey("state").foreach { k =>
      info(s"$k")
    }
  }

  test("root node") {
    mpt.rootNode("state").map { n =>
      info(s"$n")
    }
  }

  test("transact") {
    val k1: Array[Byte] = Array(0, 0, 0, 0, 0, 0, 0, 1)
    val k2: Array[Byte] = Array(0, 0, 0, 0, 0, 0, 0, 2)
    val v: Array[Byte]  = Array(1, 1, 1, 1)

    // t1 success
    val t1 = mpt.transact { proxy =>
      proxy.put(k1, v)
      proxy.put(k2, v)
    }

    assert(t1.isRight)
    val v1 = mpt.get(k2)
    assert(v1.isRight && v1.right.get.isDefined && v1.right.get.get.sameElements(v))

    // t2 try to update k2 -> v2, but transaction failed
    val q: Array[Byte] = Array(2,2,2,2)
    val t2 = mpt.transact {proxy =>
      proxy.put(k1, q)
      throw new RuntimeException("break the transaction")
      proxy.put(k2, q)
    }

    assert(t2.isLeft)
    val v2 = mpt.get(k2)
    assert(v2.isRight && v2.right.get.isDefined && v2.right.get.get.sameElements(v))
  }

}

object MPTSpec {

}
