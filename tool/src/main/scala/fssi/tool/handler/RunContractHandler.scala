package fssi
package tool
package handler

import java.io.File

import ast._
import fssi.ast.components.Model.Op
import uc._
import fssi.interpreter.Setting.ToolSetting
import fssi.interpreter.runner
import fssi.types.CodeFormat
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created on 2018/8/27
  */
trait RunContractHandler {

  val logger: Logger               = LoggerFactory.getLogger(getClass)
  val toolProgram: ToolProgram[Op] = ToolProgram[components.Model.Op]

  val setting: ToolSetting = ToolSetting()

  def apply(classesDir: File,
            qualifiedClassName: String,
            methodName: String,
            parameters: Array[String],
            decodeFormat: CodeFormat): Unit = {
    runner
      .runIOAttempt(
        toolProgram
          .runContract(classesDir.toPath, qualifiedClassName, methodName, parameters, decodeFormat),
        setting)
      .unsafeRunSync() match {
      case Right(_) ⇒ logger.info(s"invoke method [$qualifiedClassName#$methodName] success")
      case Left(e)  ⇒ e.printStackTrace()
    }
  }
}
