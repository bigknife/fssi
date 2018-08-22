package fssi
package corenode
package handler

import types._
import interpreter._
import types.syntax._
import ast._, uc._
import utils._

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import json.implicits._

import org.slf4j._

trait StartupHandler extends JsonMessageHandler {
  private val log = LoggerFactory.getLogger(getClass)

  private val coreNodeProgram                            = CoreNodeProgram[components.Model.Op]
  private val settingOnce: Once[Setting.CoreNodeSetting] = Once.empty

  // message type names can be handled
  private val acceptedTypeNames: Vector[String] = Vector(
    JsonMessage.TYPE_NAME_TRANSACTION // then, the body will be parsed to a Transaction object
  )

  def apply(setting: Setting.CoreNodeSetting): Unit = {
    val node =
      runner.runIO(coreNodeProgram.startup(setting.workingDir, this), setting).unsafeRunSync
    settingOnce := setting

    // add shutdown hook to clean resources.
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIO(coreNodeProgram.shutdown(node), setting).unsafeRunSync
    }))

    // long running.
    Thread.currentThread.join()
  }

  def showNode(node: Node): String = node.toString

  def ignored(message: JsonMessage): Boolean = !acceptedTypeNames.contains(message.typeName)

  def handle(jsonMessage: JsonMessage): Unit = {
    import JsonMessage._
    jsonMessage.typeName match {
      case TYPE_NAME_TRANSACTION =>
        val transactionResult = for {
          json        <- parse(jsonMessage.body)
          transaction <- json.as[Transaction]
        } yield transaction

        transactionResult match {
          case Left(t)            => log.error("transaction json deserialization faield", t)
          case Right(transaction) =>
            //TODO: handle transaction
            log.debug(s"start handle transaction: $transaction")
            settingOnce foreach { setting =>
              runner.runIO(coreNodeProgram.handleTransaction(transaction), setting).unsafeRunSync
            }
            log.debug(s"handled transaction: ${transaction.id}")

        }

      case x => throw new RuntimeException(s"Unsupport TypeName: $x")
    }
  }

}
