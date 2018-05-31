package bigknife.jsonrpc

import io.circe.Json

/**
  * Invokable JsonRpc resource.
  */
trait Resource {
  /** if method included in current resource
    *
    */
  def contains(method: String): Boolean

  /** invoke method with parameters */
  def invoke(method: String, params: Json): Either[Throwable, Json]
}
