package fssi
package edgenode

import types._, exception._
import interpreter._, util._
import types.syntax._
import ast._, uc._

import io.circe._
import io.circe.syntax._
import json.implicits._
import bigknife.jsonrpc._

import scala.util._
import org.slf4j._

import EdgeJsonRpcResource.methods._

trait EdgeJsonRpcResource extends Resource {
  val setting: Setting.EdgeNodeSetting
  val edgeNodeProgram: EdgeNodeProgram[components.Model.Op]

  private lazy val log = LoggerFactory.getLogger(getClass)

  /** check the method if jsonrpc resources supported
    */
  def contains(method: String): Boolean = acceptable(method)

  /** check params whether they can be accepted or not.
    *
    */
  def paramsAcceptable(method: String, params: Json): Boolean = method match {
    case SEND_TRANSACTION =>
      if (params.isObject) {
        val jso = params.asObject.get
        jso("type") match {
          case Some(t) if t.isString =>
            t.asString match {
              case Some("Transfer")        => true
              case Some("PublishContract") => true
              case Some("RunContract")     => true
              case _                       => false
            }
          case None => false
        }
      } else false
  }

  def invoke(method: String, params: Json): Either[Throwable, Json] = {
    if (log.isDebugEnabled) log.debug(s"invoking: $method, with params: ${params.noSpaces}")
    Try {
      val jsonMessage: Option[JsonMessage] = method match {
        case SEND_TRANSACTION =>
          Some(JsonMessage(JsonMessage.TYPE_NAME_TRANSACTION, params.noSpaces))
        case _ => None
      }
      if (jsonMessage.isDefined) {
        runner.runIO(edgeNodeProgram.broadcastMessage(jsonMessage.get), setting).unsafeRunSync
        if (log.isDebugEnabled) log.debug(s"invoking $method succcessfully")
        Json.fromString("accepted")
      } else throw new FSSIException(s"Unsupport method: $method")
    }.toEither
  }
}

object EdgeJsonRpcResource {
  object methods {
    val SEND_TRANSACTION = "sendTransaction"

    def acceptable(method: String): Boolean = method match {
      case SEND_TRANSACTION => true
      case _                => false
    }
  }
}
