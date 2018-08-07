package fssi
package edgenode
package handler

import types._
import interpreter._, util._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._

import bigknife.jsonrpc._

trait StartupHandler extends JsonMessageHandler {
  val edgeNodeProgram = EdgeNodeProgram[components.Model.Op]

  def apply(setting: Setting.EdgeNodeSetting): Unit = {
    val node = runner.runIO(edgeNodeProgram.startup(this), setting).unsafeRunSync

    // json rpc server
    val configReader = ConfigReader(setting.configFile)
    server.run(
      name = "edge",
      version = "v1",
      resource = jsonrpcResource(setting, edgeNodeProgram),
      host = configReader.readJsonRpcHost(),
      port = configReader.readJsonRpcPort()
    )

    // add shutdown hook to clean resources.
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIO(edgeNodeProgram.shutdown(node), setting).unsafeRunSync
    }))

    // long running.
    Thread.currentThread.join()
  }

  def ignored(message: JsonMessage): Boolean = {
    // edgenode only broadcast, don't handle any messages.
    true
  }

  def handle(jsonMessage: JsonMessage): Unit = {
    println(s"Got JsonMessage: $jsonMessage")
  }

}
