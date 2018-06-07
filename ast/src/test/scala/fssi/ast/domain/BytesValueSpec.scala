package fssi.ast.domain

import fssi.ast.domain.types.BytesValue
import org.scalatest.FunSuite

class BytesValueSpec extends FunSuite{
  test("hex and decodeHex") {
    val arr = "Hello,world".getBytes

    val hex = BytesValue(arr).hex
    info(hex)

    val decoded = BytesValue.decodeHex(hex).utf8String
    info(decoded)

    assert(decoded == "Hello,world")
  }
}
