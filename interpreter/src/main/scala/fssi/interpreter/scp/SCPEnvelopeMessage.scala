package fssi
package interpreter
package scp

import types._
import bigknife.scalap.ast.types.{Envelope, Message}

/** scp will sync every node's quorum set
  */
case class SCPEnvelopeMessage(envelope: Envelope[Message]) extends ConsensusAuxMessage
