package fssi.scp

import fssi.utils._
import fssi.scp.types._

trait TestSupport {
  crypto.registerBC()

  def createNodeID(): NodeID = {
    val kp = crypto.generateECKeyPair("secp256k1")
    val pk = crypto.getECPublicKey(kp)
    NodeID(pk)
  }
}
