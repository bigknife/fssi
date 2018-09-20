package fssi
package tool
package handler

import types.biz._
import types.base._
import types.implicits._
import interpreter._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import jsonCodecs._

trait CreateAccountHandler extends BaseHandler {

  def apply(password: String): Unit = {
    val setting: Setting = Setting.ToolSetting()
    val (account,sk)          = runner.runIO(toolProgram.createAccount(RandomSeed(password.getBytes("utf-8"))), setting).unsafeRunSync
    println(showAccount(account))
  }

  private def showAccount(account: Account): String = account.asJson.spaces2
}
