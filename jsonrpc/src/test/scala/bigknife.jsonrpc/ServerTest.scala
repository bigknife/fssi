package bigknife.jsonrpc

import io.circe.Json

object ServerTest extends App {

  val resource = new Resource {
    /** if method included in current resource
      *
      */
    override def contains(method: String): Boolean = method == "test"

    /** invoke method with parameters */
    override def invoke(method: String, params: Json): Either[Throwable, Json] = {
      Right(Json.fromString("test result"))
    }

    /** can the params be accepted
      *
      */
    override def paramsAcceptable(method: String, params: Json): Boolean = true
  }

  server.run("test", "v1", resource)
  Thread.currentThread().join()
}
