package bigknife.jsonrpc

import io.circe.Json

/**
  * json rpc request object
  *
  * @see http://www.jsonrpc.org/specification#request_object
  */
case class Request[A](
    id: String,
    method: String,
    params: A,
    jsonrpc: String = Version
)

object Request {
  trait Implicits {
    import io.circe.Encoder
    implicit def requestJsonEncoder[A](implicit AE: Encoder[A]): Encoder[Request[A]] =
      (r: Request[A]) => {
        Json.obj(
          "id"      -> Json.fromString(r.id),
          "method"  -> Json.fromString(r.method),
          "params"  -> AE(r.params),
          "jsonrpc" -> Json.fromString(r.jsonrpc)
        )
      }
  }
  object implicits extends Implicits
}
