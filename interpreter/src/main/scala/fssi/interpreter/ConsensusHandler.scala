package fssi
package interpreter
import fssi.ast.Consensus
import fssi.interpreter.Setting.CoreNodeSetting
import fssi.interpreter.scp.SCPSupport
import fssi.scp._
import fssi.types.biz.Node.ConsensusNode
import fssi.types.biz.{ConsensusAuxMessage, Receipt, Transaction}

class ConsensusHandler extends Consensus.Handler[Stack] with SCPSupport {

  override def initialize(node: ConsensusNode): Stack[Unit] = Stack { setting =>
    setting match {
      case coreNodeSetting: CoreNodeSetting =>
        val consensusConfig                          = coreNodeSetting.config.consensusConfig
        implicit val scpSetting: interpreter.Setting = resolveSCPSetting(consensusConfig)
        Portal.initialize
      case _ =>
    }
  }

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
