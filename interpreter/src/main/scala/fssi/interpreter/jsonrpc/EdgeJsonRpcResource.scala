package fssi.interpreter.jsonrpc

import bigknife.jsonrpc._
import fssi.base.implicits._
import fssi.interpreter.LogSupport
import fssi.types.biz.{Message, Transaction}
import fssi.types.biz.Message.ClientMessage
import fssi.types.biz.Message.ClientMessage.{QueryTransaction, SendTransaction}
import io.circe._

import scala.util._
import fssi.types.json.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import fssi.interpreter.scp.BlockValue.implicits._

class EdgeJsonRpcResource(clientMessageHandler: Message.Handler[ClientMessage, Transaction])
    extends Resource
    with LogSupport
    with RequestProtocol {

  /** check the method if jsonrpc resources supported
    */
  def contains(method: String): Boolean = method match {
    case m if m == SEND_TRANSACTION || m == QUERY_TRANSACTION => true
    case _                                                    => false
  }

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
    case QUERY_TRANSACTION =>
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
          val sendTransaction = SendTransaction(params.noSpaces.asBytesValue.bytes)
          clientMessageHandler(sendTransaction)
          Json.fromString("request accepted")
        case QUERY_TRANSACTION =>
          val queryTransaction = QueryTransaction(params.noSpaces.asBytesValue.bytes)
          val transaction      = clientMessageHandler(queryTransaction)
          transaction.asJson
        case x => throw new UnsupportedOperationException(s"unsupported method: $x")
      }
    }.toEither
  }
}
