package fssi
package tool
package handler

import types._
import interpreter._
import types.syntax._
import ast._, uc._

import java.io._

trait CreateContractProjectHandler extends BaseHandler {

  def apply(projectDirectory: File): Unit = {
    val setting: Setting = Setting.ToolSetting()
    runner
      .runIOAttempt(toolProgram.createContractProject(projectDirectory), setting)
      .unsafeRunSync() match {
      case Left(e)  => logger.error(e.getMessage)
      case Right(_) => logger.info(s"created,please checkout $projectDirectory")
    }
  }
}
