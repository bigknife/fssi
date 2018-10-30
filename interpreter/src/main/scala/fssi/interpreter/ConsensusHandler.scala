package fssi
package interpreter
import fssi.ast.Consensus
import fssi.types.biz.Node.ConsensusNode
import fssi.types.biz.{ConsensusAuxMessage, Receipt, Transaction}

class ConsensusHandler extends Consensus.Handler[Stack] {

  override def initialize(node: ConsensusNode): Stack[Unit] = ???

  override def destroy(): Stack[Unit] = ???

  override def tryAgree(transaction: Transaction, receipt: Receipt): Stack[Unit] = ???

  override def processMessage(message: ConsensusAuxMessage): Stack[Unit] = ???
}

object ConsensusHandler {
  val instance = new ConsensusHandler

  trait Implicits {
    implicit val consensusHandler: ConsensusHandler = instance
  }
}
