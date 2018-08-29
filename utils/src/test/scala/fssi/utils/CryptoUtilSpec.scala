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
}
