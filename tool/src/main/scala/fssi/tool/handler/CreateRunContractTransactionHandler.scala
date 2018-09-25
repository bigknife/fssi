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

trait CreateRunContractTransactionHandler extends BaseHandler {
  def apply(accountFile: File,
            secretKeyFile: File,
            contractName: UniqueName,
            contractVersion: Contract.Version,
            methodAlias: String,
            parameter: Option[Contract.UserContract.Parameter]): Unit = {

    val setting: Setting = Setting.ToolSetting()

    runner
      .runIOAttempt(toolProgram.createRunTransaction(accountFile,
                                                     secretKeyFile,
                                                     contractName,
                                                     contractVersion,
                                                     methodAlias,
                                                     parameter),
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
        println(showRequest(request))
    }

  }

  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
