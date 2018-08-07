package fssi
package edgenode

import types._
import interpreter._, util._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._
import bigknife.jsonrpc._

import scala.util._

trait EdgeJsonRpcResource extends Resource {
  val setting: Setting.EdgeNodeSetting
  val edgeNodeProgram: EdgeNodeProgram[components.Model.Op]

  def contains(method: String): Boolean = true

  def paramsAcceptable(method: String, params: Json): Boolean = true

  def invoke(method: String, params: Json): Either[Throwable, Json] = {
    Try {
      runner.runIO(edgeNodeProgram.broadcastMessage(
        JsonMessage("test", Json.fromString(method).noSpaces)
      ), setting).unsafeRunSync
      Json.fromString("test")
    }.toEither
  }
}
