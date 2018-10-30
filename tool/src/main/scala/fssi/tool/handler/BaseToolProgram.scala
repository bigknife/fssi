package fssi
package tool
package handler

import fssi.ast._
import fssi.ast.uc._
import fssi.interpreter._
import org.slf4j._
import bigknife.sop._
import bigknife.sop.implicits._

trait BaseToolProgram {
  type Effect = StackConsoleMain.Effect

  lazy val logger = LoggerFactory.getLogger(getClass)
  val toolProgram = ToolProgram[blockchain.Model.Op]

  implicit def lift[A](a: A): Program[A] = a.pureSP[blockchain.Model.Op]
}
