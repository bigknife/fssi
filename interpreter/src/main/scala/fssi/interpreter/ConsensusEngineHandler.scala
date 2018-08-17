package fssi
package interpreter

import types._, exception._
import ast._
import org.slf4j.LoggerFactory

import bigknife.scalap.ast.types.{NodeID, SlotIndex}
import bigknife.scalap.interpreter.{runner => scpRunner, Setting => SCPSetting, _}
import bigknife.scalap.ast.usecase.SCP
import bigknife.scalap.ast.usecase.component.{Model => SCPModel, _}

class ConsensusEngineHandler extends ConsensusEngine.Handler[Stack] {
  private val scp: SCP[SCPModel.Op] = SCP[SCPModel.Op]

  /** initialize consensus engine
    */
  override def initialize(account: Account): Stack[Unit] = Stack { setting =>
    // only core node need run consensus
    setting match {
      case x: Setting.CoreNodeSetting =>
        val confReader = ConfigReader(x.configFile)
        val scpSetting = SCPSetting(
          localNodeID = NodeID(account.id.value.bytes),
          quorumSet = confReader.readQuorumSet(),
          connect = x.consensusConnect,
          maxTimeoutSeconds = confReader.readMaxTimeoutSeconds,
          presetQuorumSets = Map.empty
        )
        scpRunner.runIO(scp.initialize(), scpSetting).unsafeRunSync

      case _ => // nothing to do
    }
  }
}

object ConsensusEngineHandler {
  private val instance: ConsensusEngineHandler = new ConsensusEngineHandler

  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = instance
  }
}
