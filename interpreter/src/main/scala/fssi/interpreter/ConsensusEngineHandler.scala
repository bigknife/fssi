package fssi.interpreter

import bigknife.scalap.ast.types.{NodeID, SlotIndex}
import bigknife.scalap.ast.usecase.SCP
import bigknife.scalap.ast.usecase.component._
import fssi.ast.domain._
import fssi.ast.domain.types._
import fssi.interpreter.scp.{MomentValue, SCPExecutionService}
import fssi.interpreter.util.{MomentPool, Once}
import org.slf4j.LoggerFactory

class ConsensusEngineHandler extends ConsensusEngine.Handler[Stack] {
  private val log = LoggerFactory.getLogger(getClass)

  private val momentPool: Once[MomentPool] = Once.empty

  private val scp: SCP[Model.Op] = SCP[Model.Op]

  private def _init(setting: Setting): Unit = {
    momentPool := MomentPool.newPool(setting.maxMomentSize, setting.maxMomentPoolElapsedSecond)
  }

  override def init(node: Node): Stack[Unit] = Stack { setting =>
    _init(setting)
    // scp should broadcast current node quorumset periodically
    val p = scp.initialize()
    bigknife.scalap.interpreter.runner
      .runIO(p, setting.toScalapSetting(NodeID(node.accountPublicKey.bytes)))
      .unsafeRunSync()
  }

  override def poolMoment(node: Node,
                          currentHeight: BigInt,
                          previous: Vector[Moment],
                          moment: Moment): Stack[Boolean] = Stack { setting =>
    //momentPool.unsafe().push(moment)
    // directly put to scp
    //setting.
    val nodeID        = NodeID(node.accountPublicKey.bytes)
    val value         = MomentValue(moment)
    val previousValue = MomentValue(moment)
    val slotIndex     = SlotIndex(currentHeight + 1)

    val p = scp.nominate(nodeID,
                         slotIndex,
                         round = 0,
                         valueToNominate = value,
                         previousValue = previousValue)
    bigknife.scalap.interpreter.runner.runIO(p, setting.toScalapSetting(nodeID)).unsafeRunSync()
  }
}
object ConsensusEngineHandler {
  private val _instance = new ConsensusEngineHandler
  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = _instance
  }
  object implicits extends Implicits
}
