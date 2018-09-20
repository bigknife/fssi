package fssi
package interpreter

import org.scalatest._
import types.implicits._
import types.base._
import types.biz._
import utils._

class AccountServiceHandlerSpec extends FunSuite {
  crypto.registerBC()


  test("createSecp256k1KeyPair") {
    val (pubKey, privKey) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync

    info(s"pubkey = ${pubKey.asBytesValue.bcBase58}")
    info(s"privkey = ${privKey.asBytesValue.bcBase58}")
  }

  test("createAessecretkey") {
    val seed = RandomSeed("Hello,world! I love peace.".getBytes)
    val aesKey =
      AccountServiceHandler.instance.createAesSecretKey(seed)(Setting.DefaultSetting).unsafeRunSync
    info(s"aesKey = ${aesKey.asBytesValue.bcBase58}")
  }

  test("aesEncryptprivKey") {
    val (_, privKey) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync
    val seed = RandomSeed("abcdefg,hello,world".getBytes)
    val aesKey =
      AccountServiceHandler.instance.createAesSecretKey(seed)(Setting.DefaultSetting).unsafeRunSync

    val iv = AccountServiceHandler.instance.createAesIV()(Setting.DefaultSetting).unsafeRunSync

    val aesEncryptedPrivKey = AccountServiceHandler.instance
      .aesEncryptPrivKey(privKey, aesKey, iv)(Setting.DefaultSetting)
      .unsafeRunSync

    val aesEncryptedPrivKey2 = AccountServiceHandler.instance
      .aesEncryptPrivKey(privKey, aesKey, iv)(Setting.DefaultSetting)
      .unsafeRunSync

    info(s"iv = ${iv.asBytesValue.bcBase58}")
    info(s"aesEncryptedPrivKey  = ${aesEncryptedPrivKey.asBytesValue.bcBase58}")
    info(s"aesEncryptedPrivKey2 = ${aesEncryptedPrivKey2.asBytesValue.bcBase58}")
  }

  test("doubleHash and base58 wrapper") {
    val (pubKey, _) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync
    val hash =
      AccountServiceHandler.instance.doubleHash(pubKey)(Setting.DefaultSetting).unsafeRunSync
    info(s"hash = ${hash.asBytesValue.bcBase58}")

    val wrapper = AccountServiceHandler.instance
      .base58checkWrapperForAccountId(hash)(Setting.DefaultSetting)
      .unsafeRunSync
    info(s"wrapper = ${wrapper.asBytesValue.bcBase58}")
  }

}
