package fssi.interpreter.jsonrpc

trait RequestProtocol {

  lazy val SEND_TRANSACTION  = "sendTransaction"
  lazy val QUERY_TRANSACTION = "queryTransaction"
}

object RequestProtocol extends RequestProtocol
