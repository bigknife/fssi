package fssi
package interpreter
import fssi.ast.Consensus

class ConsensusHandler extends Consensus.Handler[Stack] {}

object ConsensusHandler {
  val instance = new ConsensusHandler

  trait Implicits {
    implicit val consensusHandler: ConsensusHandler = instance
  }
}
