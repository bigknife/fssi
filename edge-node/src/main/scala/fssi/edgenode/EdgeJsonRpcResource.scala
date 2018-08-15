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

  def contains(method: String): Boolean = method match {
    case "sendTransaction" => true
    case _ => false
  }

  def paramsAcceptable(method: String, params: Json): Boolean = method match {
    case "sendTransaction" =>
      if (params.isObject) {
        val jso = params.asObject.get
        jso("type") match {
          case Some(t) if t.isString =>
            t.asString match {
              case Some("Transfer") => true
              case Some("PublishContract") => true
              case Some("RunContract") => true
              case _ => false
            }
          case None => false
        }
      } else false


  }

  def invoke(method: String, params: Json): Either[Throwable, Json] = {
    Try {
      runner.runIO(edgeNodeProgram.broadcastMessage(
        JsonMessage("test", Json.fromString(method).noSpaces)
      ), setting).unsafeRunSync
      Json.fromString("test")
    }.toEither
  }
}
