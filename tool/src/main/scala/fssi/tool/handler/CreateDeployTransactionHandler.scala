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

import bigknife.jsonrpc._, Request.implicits._

import java.io._

trait CreateDeployTransactionHandler extends BaseHandler {
  def apply(accountFile: File,
            secretKeyFile: File,
            contractFile: File,
            outputFile: Option[File]): Unit = {
    val setting: Setting = Setting.ToolSetting()

    runner
      .runIOAttempt(toolProgram.createDeployTransaction(accountFile, secretKeyFile, contractFile),
                    setting)
      .unsafeRunSync match {
      case Left(t) =>
        println(t.getMessage)
      case Right(transaction) =>
        val request = Request(
          id = transaction.id.asBytesValue.bcBase58,
          method = "sendTransaction",
          params = transaction: Transaction
        )
        val output = showRequest(request)
        if (outputFile.isEmpty) println(output)
        else {
          better.files.File(outputFile.get.toPath).overwrite(output)
          ()
        }
    }
  }

  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
