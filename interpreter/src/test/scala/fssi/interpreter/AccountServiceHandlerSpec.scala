package fssi
package interpreter

import java.security.SecureRandom


import org.scalatest._
import types.implicits._
import types.base._
import types.biz._

import utils.crypto

class AccountServiceHandlerSpec extends FunSuite {
  crypto.registerBC()


  ignore("createSecp256k1KeyPair") {
    val (pubKey, privKey) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync

    info(s"pubkey = ${pubKey.asBytesValue.bcBase58}")
    info(s"privkey = ${privKey.asBytesValue.bcBase58}")
  }

  ignore("createAessecretkey") {
    for (i <- 1 to 100) {
      val seed = RandomSeed("Hello,world! I love peace.".getBytes)
      val aesKey =
        AccountServiceHandler.instance.createAesSecretKey(seed)(Setting.DefaultSetting).unsafeRunSync
      assert(aesKey.asBytesValue.length == 32)
      info(s"aesKey = ${aesKey.asBytesValue.bcBase58}")
    }

  }

  ignore("aesEncryptprivKey") {
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

  test("aesDecryptprivkey") {
    /*
    val source = Vector.fill[Char](122)('H').mkString("").getBytes()

    val key = crypto.createAesSecretKey("hello".getBytes()).getEncoded
    val iv = Array.fill[Byte](16)(1)
    val sc = new SecureRandom()
    sc.nextBytes(iv)

    val enc = crypto.aesEncryptPrivKey(iv, key, source)
    val dec = crypto.aesDecryptPrivKey(iv, key, enc)

    info(s"enc:    ${enc.asBytesValue.hex}, ${enc.length}")
    info(s"dec:    ${dec.asBytesValue.hex}")
    info(s"source: ${source.asBytesValue.hex}")
    */

    /*
    val (_, privKey) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync
    //val source = "hello,world".getBytes()
    val source = Vector.fill[Char](16)('H').mkString("").getBytes()
    //val source = privKey.value

    val seed = RandomSeed("abcdefg,hello,world".getBytes)
    val aesKey =
      AccountServiceHandler.instance.createAesSecretKey(seed)(Setting.DefaultSetting).unsafeRunSync

    val sc = new SecureRandom()
    val keyPrime = crypto.createAesSecretKey("hello".getBytes()).getEncoded
    val key = keyPrime //aesKey.value

    val iv = Array.fill[Byte](16)(1) //"1111111111111111".getBytes()
    sc.nextBytes(iv)
    val enc = crypto.aesEncryptPrivKey(iv, key, source)

    val ivs = iv.asBytesValue.bcBase58
    val keys = key.asBytesValue.bcBase58
    val encs = enc.asBytesValue.bcBase58

    info(s"ivs = $ivs, keys = $keys, encs = $encs")


    val iv1 = BytesValue.decodeBcBase58(ivs).get.bytes
    val key1 = BytesValue.decodeBcBase58(keys).get.bytes
    val enc1 = BytesValue.decodeBcBase58(encs).get.bytes


    val dec = crypto.aesDecryptPrivKey(iv, key, enc)
    val b = dec sameElements source
    info(s"$b")
    */
    /*
    val (_, privKey) =
      AccountServiceHandler.instance.createSecp256k1KeyPair()(Setting.DefaultSetting).unsafeRunSync
    val seed = RandomSeed("abcdefg,hello,world".getBytes)
    val aesKey =
      AccountServiceHandler.instance.createAesSecretKey(seed)(Setting.DefaultSetting).unsafeRunSync

    val iv = AccountServiceHandler.instance.createAesIV()(Setting.DefaultSetting).unsafeRunSync

    val aesEncryptedPrivKey = AccountServiceHandler.instance
      .aesEncryptPrivKey(privKey, aesKey, iv)(Setting.DefaultSetting)
      .unsafeRunSync
    */

    val encKey: Account.PrivKey =
      Account.PrivKey(BytesValue.decodeBcBase58("24fjKFXVuVR5ahtHDVEwa85TCJeyvAp87qB9rTp4AgmXvcudYRxhTyuX49dHPG3v7J").get.bytes)
    val secretKey: Account.SecretKey =
      Account.SecretKey(BytesValue.decodeBcBase58("3HP5ZwGWdNhKgKydkWhab6FtkxZcV1VbFQMcCYMW3Spz").get.bytes)
    val iv: Account.IV =
      Account.IV(BytesValue.decodeBcBase58("DNNff22rSfjCQ5LDtFjcsS").get.bytes)



    val privKeyEither = AccountServiceHandler.instance
      .aesDecryptPrivKey(encKey, secretKey, iv)(Setting.DefaultSetting)
      .unsafeRunSync

    privKeyEither match {
      case Left(t) =>
        t.printStackTrace()
      case Right(pk) => info(pk.asBytesValue.bcBase58)
    }


    assert(privKeyEither.isRight)
  }

  ignore("doubleHash and base58 wrapper") {
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
