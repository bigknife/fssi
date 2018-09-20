package fssi
package types
package base

import org.scalatest._
import fssi.types.base.implicits._

class Base58CheckSpec extends FunSuite {
  test("Base58Check sanity") {
    val b = Base58Check(1.toByte, "Hello,world".getBytes)
    assert(!b.isSane)

    val b1 = b.resetChecksum
    assert(b1.isSane)
  }
}
