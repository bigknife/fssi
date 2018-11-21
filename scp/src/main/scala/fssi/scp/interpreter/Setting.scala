package fssi.scp
package interpreter

import fssi.scp.types._

case class Setting(
    initFakeValue: Value,
    maxTimeoutSeconds: Int = 30 * 60, // max neutralization time
    quorumSet: QuorumSet, // quorum set, only support Slices now
    localNode: NodeID, // local Node ID
    privateKey: java.security.PrivateKey, // used to sign and verify
    applicationCallback: ApplicationCallback, // application level callback
    broadcastTimeout: Long // timeout to broadcast all of nominate and ballot messages
)
