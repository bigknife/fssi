package fssi
package edgenode

import bigknife.jsonrpc._
import fssi.edgenode.EdgeJsonRpcResource.methods._
import io.circe._
import org.slf4j._

import scala.util._

trait EdgeJsonRpcResource extends Resource {

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

  def invoke(method: String, params: Json): Either[Throwable, Json] = ???
}

object EdgeJsonRpcResource extends EdgeJsonRpcResource {
  object methods {
    val SEND_TRANSACTION = "sendTransaction"

    def acceptable(method: String): Boolean = method match {
      case SEND_TRANSACTION => true
      case _                => false
    }
  }
}
