package fssi
package interpreter
package scp

import types._

/** scp will sync every node's quorum set
  */
case class QuorumSetSyncMessage(quorumSetSync: QuorumSetSync) extends ConsensusAuxMessage
