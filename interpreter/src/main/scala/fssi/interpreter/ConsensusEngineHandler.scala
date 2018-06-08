package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._
import fssi.interpreter.util.{MomentPool, Once}

class ConsensusEngineHandler extends ConsensusEngine.Handler[Stack] {

  private val momentPool: Once[MomentPool] = Once.empty

   def init(setting: Setting): Unit = {
     momentPool := MomentPool.newPool(setting.maxMomentSize, setting.maxMomentPoolElapsedSecond)
   }

  override def poolMoment(moment: Moment): Stack[Boolean] = Stack { setting =>
    init(setting)
    momentPool.unsafe().push(moment)
  }
}
object ConsensusEngineHandler {
  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = new ConsensusEngineHandler
  }
  object implicits extends Implicits
}
