package fssi
package interpreter

import org.scalatest._
import types._
import types.syntax._

class CryptoHandlerTest extends FunSuite with GivenWhenThen {
  val setting = Setting.DefaultSetting
  val crypto  = new CryptoHandler

  test("create keypair") {

    val kp = crypto
      .createKeyPair()
      .map(x => (x._1.hex, x._2.hex))
      .run(setting)
      .unsafeRunSync()

    info(s"created keypair is $kp")
  }

  test("create iv fork des") {
    val iv = crypto
      .createIVForDes()
      .map(_.hex)
      .run(setting)
      .unsafeRunSync()

    info(s"created iv is $iv")
  }

  test("encrypt private key") {
    Given("KeyPair")
    val kp = crypto
      .createKeyPair()
      .run(setting)
      .unsafeRunSync()
    info(s"private key is ${kp._2.hex}")

    Given("IV")
    val iv = crypto
      .createIVForDes()
      .run(setting)
      .unsafeRunSync()

    When("encrypt private key of there KeyPair with a password")
    val password = "Hello,world".getBytes

    Then("do encrypt")
    val result = crypto
      .desEncryptPrivateKey(kp._2, iv, password)
      .run(setting)
      .unsafeRunSync
    info(s"result is ${result.hex}")
  }
}
