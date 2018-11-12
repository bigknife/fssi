package fssi.scp
package interpreter

import types._

case class Setting(
    nodeId: NodeID,
    maxTimeoutSeconds: Int = 30 * 60, // max neutralization time
    quorumSet: QuorumSet, // quorum set, only support Slices now
    privateKey: java.security.PrivateKey, // used to sign and verify
    applicationCallback: ApplicationCallback // application level callback
)
