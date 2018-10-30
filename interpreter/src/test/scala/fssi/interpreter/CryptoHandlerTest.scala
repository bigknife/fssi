package fssi
package interpreter

import utils._
import org.scalatest._
import types.{base, _}

class CryptoHandlerTest extends FunSuite with GivenWhenThen {
  val setting = Setting.DefaultSetting
  val cryptoHandler = {
    utils.crypto.registerBC()
    new CryptoHandler
  }

  test("sign and verify") {
    val encrytedKey = "ADYTEporUmFmKToDQHBc76hXz4RfoRKZUkuUAnpa47aXYHnELmVknH5s2QvTAaBfHU"
    val pubKey      = "21884yKTVyHonyERqEUuThnZo1eBe9YtF14NRxLPhT9yu"
    val iv          = "QoVapk76GcfUaCxYUQvT9W"
    val secretKey   = "C7absPeDYLoMAzQuVyrA8cyvXSMeuJNPkAz96SNZDQri"

    val privKey = for {
      ivBytes        <- base.BytesValue.decodeBcBase58[Any](iv)
      secretKeyBytes <- base.BytesValue.decodeBcBase58[Any](secretKey)
      encrytionBytes <- base.BytesValue.decodeBcBase58[Any](encrytedKey)
    } yield {
      val pkBytes =
        crypto.aesDecryptPrivKey(ivBytes.bytes, secretKeyBytes.bytes, encrytionBytes.bytes)
      crypto.rebuildECPrivateKey(pkBytes, crypto.SECP256K1)
    }

    val data = "Hello,WOrld".getBytes()
    val sign = crypto.makeSignature(data, privKey.get)
    val verify = crypto.verifySignature(
      sign,
      data,
      crypto.rebuildECPublicKey(base.BytesValue.decodeBcBase58(pubKey).get.bytes, crypto.SECP256K1))
    assert(verify)

  }
}
