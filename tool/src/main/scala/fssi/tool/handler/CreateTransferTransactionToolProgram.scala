package fssi
package tool
package handler

import types.base._
import types.biz._
import types.exception._
import types.implicits._

import interpreter._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import jsonCodecs._
import io.circe.generic.auto._

import bigknife.jsonrpc._, Request.implicits._
import java.io._

trait CreateTransferTransactionToolProgram extends BaseToolProgram {

  def apply(accountFile: File,
            secretKeyFile: File,
            payee: Account.ID,
            token: Token,
            outputFile: Option[File]): Effect = {

    for {
      transfer <- toolProgram.createTransferTransaction(accountFile, secretKeyFile, payee, token)
      _        <- handleTransfer(transfer, outputFile)
    } yield ()
  }
  private def handleTransfer(transfer: Transaction.Transfer, outputFile: Option[File]): Unit = {
    val request = Request(
      id = transfer.id.asBytesValue.bcBase58,
      method = "sendTransaction",
      params = transfer: Transaction
    )
    val output = showRequest(request)
    if (outputFile.isEmpty) println(output)
    else {
      better.files.File(outputFile.get.toPath).overwrite(output)
      ()
    }
  }
  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
