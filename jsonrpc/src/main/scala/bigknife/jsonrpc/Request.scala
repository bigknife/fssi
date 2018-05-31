package bigknife.jsonrpc

/**
  * json rpc request object
  * @see http://www.jsonrpc.org/specification#request_object
  */
case class Request[A](
    id: String,
    method: String,
    params: A,
    jsonrpc: String = Version
)
