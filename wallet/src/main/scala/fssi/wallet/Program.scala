package fssi.wallet

import fssi.ast._
import fssi.ast.uc._
import fssi.interpreter._
import org.slf4j._
import bigknife.sop._
import bigknife.sop.implicits._

trait Program {
  val toolProgram: ToolProgram[blockchain.Model.Op] = ToolProgram[blockchain.Model.Op]
}

object Program extends Program
