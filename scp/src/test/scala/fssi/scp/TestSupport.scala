package fssi.scp

import java.security.PrivateKey

import fssi.scp.types._
import fssi.utils._

trait TestSupport {
  crypto.registerBC()

  def createNodeID(): (NodeID, PrivateKey) = {
    val kp = crypto.generateECKeyPair(cryptoUtil.SECP256K1)

    (NodeID(crypto.getECPublicKey(kp)), kp.getPrivate)
  }


}
