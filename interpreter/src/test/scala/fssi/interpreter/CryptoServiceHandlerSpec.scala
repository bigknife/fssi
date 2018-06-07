package fssi.interpreter

import fssi.ast.domain.types.BytesValue
import org.scalatest.FunSuite

class CryptoServiceHandlerSpec extends FunSuite {
  val crypto           = new CryptoServiceHandler
  val setting: Setting = Setting()

  test("encrypt and decrypt") {
    for (_ <- 1 to 10) {
      val source = BytesValue("hello,world")
      val key    = BytesValue("12345678")
      val iv     = BytesValue(crypto.randomChar(8).map(_.map(_.toByte)).run(setting).unsafeRunSync())
      val key1   = crypto.enforceDes3Key(key)(setting).unsafeRunSync()
      val s      = crypto.des3cbcEncrypt(source, key1, iv)(setting).unsafeRunSync()
      info(s"dest = ${s.hex}")
      info(s"iv = ${iv.hex}")

      val decoded = BytesValue.decodeHex(s.hex)
      assert(decoded == s)

      val r = crypto.des3cbcDecrypt(decoded, key1, iv).run(setting).unsafeRunSync()
      info(r.utf8String)
      assert(r.utf8String == source.utf8String)

      // due to result
      val encrypted = "6d29163c59da92ae32bbbddc329a726f"
      val iv1       = BytesValue.decodeHex("777a796e646e7371")
      val r1 = crypto
        .des3cbcDecrypt(BytesValue.decodeHex(encrypted), key1, iv1)
        .run(setting)
        .unsafeRunSync()
      info(r1.utf8String)

    }
  }

  test("decrypt account's private key") {
    val source = BytesValue
      .decodeHex("ae028f941fd395e53f5044c588aeb9c1ef6fb94b95232a93bcf411cd75603ebb5f6d8e4f133b5a54")
    val pass = crypto.enforceDes3Key(BytesValue("a!123")).run(setting).unsafeRunSync()
    val iv   = BytesValue.decodeHex("66796a6374716474")
    val r    = crypto.des3cbcDecrypt(source, pass, iv).run(setting).unsafeRunSync()
    info(r.hex)
  }
}
