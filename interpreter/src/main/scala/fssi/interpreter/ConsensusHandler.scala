package fssi
package interpreter
import fssi.ast.Consensus
import fssi.types.biz.{Node, Receipt, Transaction}

class ConsensusHandler extends Consensus.Handler[Stack] {

  override def initialize(node: Node): Stack[Unit] = ???

  override def destroy(): Stack[Unit] = ???

  override def tryAgree(transaction: Transaction, receipt: Receipt): Stack[Unit] = ???
}

object ConsensusHandler {
  val instance = new ConsensusHandler

  trait Implicits {
    implicit val consensusHandler: ConsensusHandler = instance
  }
}
