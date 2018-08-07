package bigknife.jsonrpc

import io.circe.Json.JString
import io.circe.{Json, JsonObject}

/**
  * @see http://www.jsonrpc.org/specification#response_object
  */
sealed trait Response[A] {
  def jsonrpc: String
  def id: Option[String]
}

object Response {
  type CodeMessage = (Int, String)

  case class ErrorInfo[A](
      code: Int,
      message: String,
      data: Option[A]
  )

  case class Error[A](
      id: Option[String],
      error: ErrorInfo[A],
      jsonrpc: String = Version
  ) extends Response[A]

  case class Success[A](
      id: Option[String],
      result: A,
      jsonrpc: String = Version
  ) extends Response[A]

  /** make a success response */
  def success[A](id: String, result: A): Response[A] = Success(
    Some(id),
    result
  )

  /** make an error response*/
  def error[A](id: Option[String], code: Int, message: String, data: Option[A] = None): Response[A] = Error(
    id,
    ErrorInfo(
      code,
      message,
      data
    )
  )

  private def _error[A](codeMessage: CodeMessage, data: Option[A] = None): Response[A] =
    error(None, codeMessage._1, codeMessage._2, data)

  //// some pre-defined errors
  object errors {

    val ParseError: CodeMessage     = -32700 -> "Parse error"
    val InvalidRequest: CodeMessage = -32600 -> "Invalid Request"
    val MethodNotFound: CodeMessage = -32601 -> "Method not found"
    val InvalidParams: CodeMessage  = -32602 -> "Invalid params"
    val InternalError: CodeMessage  = -32603 -> "Internal error"

  }
  import errors._
  def parseError[A](data: Option[A] = None): Response[A] = _error(ParseError, data)
  def invalidRequest[A](data: Option[A] = None): Response[A] =
    _error(InvalidRequest, data)
  def methodNotFound[A](data: Option[A] = None): Response[A] =
    _error(MethodNotFound, data)
  def invalidParams[A](data: Option[A] = None): Response[A] =
    _error(InvalidParams, data)
  def internalError[A](data: Option[A] = None): Response[A] =
    _error(InternalError, data)

  trait Implicits {
    import io.circe.Encoder
    implicit def circeJsonEncoder[A](implicit AE: Encoder[A]): Encoder[Response[A]] = {
      case x: Success[A] =>
        val seq: Seq[(String, Json)] =
          x.id.map("id" -> Json.fromString(_)).toSeq ++
            Seq("jsonrpc" -> Json.fromString(x.jsonrpc), "result" -> AE(x.result))
        Json.obj(seq: _*)

      case x: Error[A] =>
        val seq: Seq[(String, Json)] =
          x.id.map("id" -> Json.fromString(_)).toSeq ++
            Seq(
              "jsonrpc" -> Json.fromString(x.jsonrpc),
              "error" -> Json.obj(
                x.error.data.map("data" -> AE(_)).toSeq ++
                  Seq("code"    -> Json.fromInt(x.error.code),
                      "message" -> Json.fromString(x.error.message)): _*
              )
            )

        Json.obj(seq: _*)
    }
  }

  object implicits extends Implicits
}
