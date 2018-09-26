package fssi
package tool
package handler

import types._
import interpreter._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._
import java.io._

trait CompileContractHandler extends BaseHandler {
  def apply(accountFile: File, secretKeyFile: File, projectDirectory: File, outputFile: File, sandboxVersion: String): Unit = {
    val setting: Setting = Setting.ToolSetting()
    runner
      .runIO(toolProgram.compileContract(accountFile, secretKeyFile, projectDirectory, outputFile, sandboxVersion), setting)
      .unsafeRunSync

  }
}
