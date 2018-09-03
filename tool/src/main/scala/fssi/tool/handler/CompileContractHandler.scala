package fssi
package tool
package handler

import java.io.File

import ast._
import fssi.ast.components.Model.Op
import fssi.types.CodeFormat
import uc._
import interpreter._
import org.slf4j.{Logger, LoggerFactory}

trait CompileContractHandler {

  val logger: Logger               = LoggerFactory.getLogger(getClass)
  val toolProgram: ToolProgram[Op] = ToolProgram[components.Model.Op]
  val setting                      = Setting.ToolSetting()

  def apply(sourceDir: File, destDir: File, format: CodeFormat): Unit = {
    runner
      .runIOAttempt(toolProgram.compileContract(sourceDir.toPath, destDir.toPath, format), setting)
      .unsafeRunSync() match {
      case Right(_) ⇒
        logger.info(s"compile contract finished,please checkout file ${destDir.toString}")
      case Left(e) ⇒
        logger.error(e.getMessage, e)
    }
  }
}
