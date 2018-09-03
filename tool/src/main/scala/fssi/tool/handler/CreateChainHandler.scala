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
import org.slf4j._

trait CreateChainHandler extends BaseHandler {

  val setting: Setting.ToolSetting = Setting.ToolSetting()

  def apply(dataDir: File, chainID: String): Unit = {
    runner.runIOAttempt(toolProgram.createChain(dataDir, chainID), setting).unsafeRunSync match {
      case Left(t) => logger.error(t.getMessage)
      case Right(_) => logger.info("created")
    }
  }
}
