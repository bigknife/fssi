package fssi
package interpreter

import types._, exception._
import ast._
import org.slf4j.LoggerFactory

import bigknife.scalap.ast.types.{NodeID, SlotIndex}
import bigknife.scalap.interpreter.{runner => scpRunner}
import bigknife.scalap.ast.usecase.SCP
import bigknife.scalap.ast.usecase.component._

class ConsensusEngineHandler extends ConsensusEngine.Handler[Stack] {

  /** initialize consensus engine
    */
  override def initialize(account: Account): Stack[Unit] = Stack { setting =>

    }

}

object ConsensusEngineHandler {
  private val instance: ConsensusEngineHandler = new ConsensusEngineHandler

  trait Implicits {
    implicit val consensusEngineHandler: ConsensusEngineHandler = instance
  }
}
