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

trait CreateRunContractTransactionHandler extends BaseHandler {
  def apply(accountFile: File,
            password: Array[Byte],
            contractName: UniqueName,
            contractVersion: Version,
            method: Contract.Method,
            parameter: Contract.Parameter): Unit = {
    val setting: Setting = Setting.ToolSetting()

    runner
      .runIOAttempt(toolProgram.createRunContractTransaction(accountFile,
                                                             password,
                                                             contractName,
                                                             contractVersion,
                                                             method,
                                                             parameter),
                    setting)
      .unsafeRunSync match {
      case Left(t) =>
        println(t.getMessage)
      case Right(transaction) =>
        val request = Request(
          id = transaction.id.value,
          method = "sendTransaction",
          params = transaction: Transaction
        )
        println(showRequest(request))
    }
  }

  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
