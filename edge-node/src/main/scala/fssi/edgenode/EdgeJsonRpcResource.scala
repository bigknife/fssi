package fssi
package edgenode

import bigknife.jsonrpc._
import fssi.ast.uc.EdgeNodeProgram
import fssi.edgenode.EdgeJsonRpcResource.methods._
import io.circe._
import io.circe.syntax._
import types.json.implicits._
import org.slf4j._

import scala.util._
import fssi.base.implicits._
import fssi.interpreter.Setting
import fssi.types.biz.Message.{ApplicationMessage, ClientMessage}
import fssi.interpreter._

trait EdgeJsonRpcResource extends Resource {

  val setting: Setting

  private lazy val log = LoggerFactory.getLogger(getClass)

  private lazy val edgeNode = EdgeNodeProgram.instance

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
    case Query_TRANSACTION =>
      if (params.isObject) {
        val json = params.asObject.get
        json.contains("transactionId")
      } else false
  }

  def invoke(method: String, params: Json): Either[Throwable, Json] = {
    if (log.isDebugEnabled) log.debug(s"invoking $method with params ${params.noSpaces}")
    Try {
      method match {
        case SEND_TRANSACTION =>
          val applicationMessage = ApplicationMessage(params.noSpaces.asBytesValue.bytes)
          runner
            .runIO(edgeNode.handleApplicationMessage(applicationMessage), setting)
            .unsafeRunSync()
          Json.fromString("request accepted")
        case Query_TRANSACTION =>
          val clientMessage = ClientMessage(params.noSpaces.asBytesValue.bytes)
          val transaction =
            runner.runIO(edgeNode.handleClientMessage(clientMessage), setting).unsafeRunSync()
          transaction.asJson
        case x => throw new UnsupportedOperationException(s"unsupported method: $x")
      }
    }.toEither
  }
}

object EdgeJsonRpcResource {
  object methods {
    val SEND_TRANSACTION  = "sendTransaction"
    val Query_TRANSACTION = "queryTransaction"

    def acceptable(method: String): Boolean = method match {
      case SEND_TRANSACTION  => true
      case Query_TRANSACTION => true
      case _                 => false
    }
  }

  def apply(implicit setting: Setting): EdgeJsonRpcResource = new EdgeJsonRpcResource {
    override val setting: Setting = setting
  }
}
