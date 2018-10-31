package fssi.scp

import fssi.scp.types._
import fssi.utils._

trait TestSupport {
  crypto.registerBC()

  def createNodeID(): NodeID = {
    val kp = crypto.generateECKeyPair(cryptoUtil.SECP256K1)
    val pk = crypto.getECPublicKey(kp)
    NodeID(pk)
  }
}
