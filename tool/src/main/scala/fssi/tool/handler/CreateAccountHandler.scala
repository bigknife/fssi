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

trait CreateAccountHandler extends BaseHandler {

  def apply(password: String): Unit = {
    val setting: Setting = Setting.ToolSetting()
    val account          = runner.runIO(toolProgram.createAccount(password), setting).unsafeRunSync
    println(showAccount(account))
  }

  private def showAccount(account: Account): String = account.asJson.spaces2
}
