package fssi.ast.domain

import cats.Id

class ConsensusEngineHandler extends ConsensusEngine.Handler[Id] {}
object ConsensusEngineHandler {
  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = new ConsensusEngineHandler
  }
  object implicits extends Implicits
}
