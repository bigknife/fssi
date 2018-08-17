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

import bigknife.jsonrpc._, Request.implicits._

import java.io._

trait CreateTransferTransactionHandler {
  val toolProgram = ToolProgram[components.Model.Op]

  def apply(accountFile: File, password: Array[Byte], payee: Account.ID, token: Token): Unit = {
    val setting: Setting = Setting.ToolSetting()
    runner
      .runIOAttempt(toolProgram.createTransferTransaction(accountFile, password, payee, token),
                    setting)
      .unsafeRunSync match {
      case Left(t) =>
        println(t.getMessage)
      case Right(transfer) =>
        val request = Request(
          id = transfer.id.value,
          method = "sendTransaction",
          params = transfer: Transaction
        )
        println(showRequest(request))
    }

  }

  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
