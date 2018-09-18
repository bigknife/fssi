package fssi
package types
package base

import org.scalatest._
import fssi.types.base.implicits._

class BytesValueSpec extends FunSuite {
  test("Array[Byte] to BytesValue") {
    val hello = "Hello,world".getBytes
    info(s"${hello.asBytesValue.hex}")
    val i = Int.MaxValue
    info(s"${i.asBytesValue.hex}")

    val h1 = "hello".asBytesValue
    val h2 = ",world".asBytesValue
    info(s"${(h1 ++ h2).utf8String}")
    info(s"${(h1 ++ h2).base64}")

    implicit val md = java.security.MessageDigest.getInstance("md5")
    info(s"${(h1 ++ h2).digest.hex}")
    info(s"${(h1 ++ h2).digest.bcBase58}")

    val hash = Hash("Hello".getBytes)
    info(s"${hash.asBytesValue.bcBase58}")
  }
}
