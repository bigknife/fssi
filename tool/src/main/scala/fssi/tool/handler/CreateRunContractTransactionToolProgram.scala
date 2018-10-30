package fssi
package tool
package handler

import types.biz._
import types.base._
import types.implicits._

import interpreter._

import io.circe._
import io.circe.syntax._
import jsonCodecs._

import bigknife.jsonrpc._, Request.implicits._

import java.io._

trait CreateRunContractTransactionToolProgram extends BaseToolProgram {

  def apply(accountFile: File,
            secretKeyFile: File,
            contractName: UniqueName,
            contractVersion: Contract.Version,
            methodAlias: String,
            parameter: Option[Contract.UserContract.Parameter],
            outputFile: Option[File]): Effect = {

    for {
      transaction <- toolProgram.createRunTransaction(accountFile,
                                                      secretKeyFile,
                                                      contractName,
                                                      contractVersion,
                                                      methodAlias,
                                                      parameter)
      _ <- handleTransaction(transaction, outputFile)
    } yield ()
  }

  private def handleTransaction(transaction: Transaction, outputFile: Option[File]): Unit = {
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

  private def showRequest(request: Request[Transaction]): String = request.asJson.spaces2
}
