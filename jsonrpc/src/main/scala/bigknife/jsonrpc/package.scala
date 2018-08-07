package bigknife

package object jsonrpc {
  // we only support jsonrpc 2.0
  // see: http://www.jsonrpc.org/specification
  val Version: String = "2.0"

  // server
  val server = new Server {}

  // implicits
  object implicits extends Response.Implicits with Request.Implicits
}
