package fssi.utils

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.scalatest._

class CryptoUtilSpec extends FunSuite {


  test("rebuildPublickKey") {
    val kp = crypto.generateECKeyPair()
    val pks = BytesUtil.toHex(crypto.getECPublicKey(kp))
    info(s"pks = $pks")
    val pk = crypto.rebuildECPublicKey(BytesUtil.decodeHex(pks))
    info(s"$pk")
  }

  test("sign and verify") {
    /*
    {
  "publicKey" : "0x02b24c31dce9626cde6a4cabd1edd5706b61a5652439ce017738821e7984830a49",
  "encryptedPrivateKey" : "0x5fea9ffe7f327def36bf6283fda15bf9f9d679891595110b088c61e0a80923e19a852f474ae5a684",
  "iv" : "0x68696d676876786f"
}
     */
    val source = "hello,world".getBytes()
    val encryptedPrivateKey = BytesUtil.decodeHex("0x5fea9ffe7f327def36bf6283fda15bf9f9d679891595110b088c61e0a80923e19a852f474ae5a684")
    val iv = BytesUtil.decodeHex("0x68696d676876786f")
    val privateKey = crypto.des3cbcDecrypt(encryptedPrivateKey, crypto.ensure24Bytes(BytesValue("passw0rd")).bytes, iv)

    val sign = crypto.makeSignature(source, crypto.rebuildECPrivateKey(privateKey))
    info(BytesUtil.toHex(sign))

    val publicKey = crypto.rebuildECPublicKey(BytesUtil.decodeHex("0x02b24c31dce9626cde6a4cabd1edd5706b61a5652439ce017738821e7984830a49"))
    val r = crypto.verifySignature(sign, source, publicKey)
    assert(r)
  }
}
