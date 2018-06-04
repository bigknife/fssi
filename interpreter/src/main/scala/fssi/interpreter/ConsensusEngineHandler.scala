package fssi.interpreter

import fssi.ast.domain._
import fssi.ast.domain.types._

class ConsensusEngineHandler extends ConsensusEngine.Handler[Stack] {}
object ConsensusEngineHandler {
  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = new ConsensusEngineHandler
  }
  object implicits extends Implicits
}
