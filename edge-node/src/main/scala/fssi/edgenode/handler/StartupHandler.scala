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
import org.slf4j._

import bigknife.jsonrpc._

trait StartupHandler extends JsonMessageHandler {
  val edgeNodeProgram = EdgeNodeProgram[components.Model.Op]

  private lazy val log = LoggerFactory.getLogger(getClass)

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
    log.info(
      s"edge node jsonrpc server startup: " +
        s"http://${configReader.readJsonRpcHost()}:${configReader.readJsonRpcPort()}/jsonrpc/edge/v1")

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
    throw new RuntimeException("EdgeNode SHOULD NOT Handle any json message")
  }

}
