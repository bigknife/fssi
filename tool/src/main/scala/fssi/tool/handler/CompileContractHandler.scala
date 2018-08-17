package fssi
package tool
package handler

import java.io.File
import java.nio.file.Paths

import ast._
import fssi.ast.components.Model.Op
import fssi.types.OutputFormat
import uc._
import interpreter._
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created on 2018/8/14
  */
trait CompileContractHandler {

  val logger: Logger               = LoggerFactory.getLogger(getClass)
  val toolProgram: ToolProgram[Op] = ToolProgram[components.Model.Op]
  val setting                      = Setting.ToolSetting()

  def apply(sourceDir: File, destDir: File, format: OutputFormat): Unit = {
    runner
      .runIOAttempt(toolProgram.compileContract(Paths.get(sourceDir.getAbsolutePath),
                                                Paths.get(destDir.getAbsolutePath),
                                                format),
                    setting)
      .unsafeRunSync() match {
      case Right(_) ⇒
        logger.info(s"compile contract finished,please checkout file ${destDir.toString}")
      case Left(e) ⇒ logger.error(e.getMessage, e)
    }
  }
}
