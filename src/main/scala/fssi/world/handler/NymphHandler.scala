package fssi.world.handler

import fssi.world.Args.NymphArgs
import bigknife.jsonrpc._
import bigknife.jsonrpc.implicits._
import io.circe.Json

class NymphHandler extends ArgsHandler[NymphArgs] {

  override def run(args: NymphArgs): Unit = {
    server.run(name = args.serviceName,
               version = args.serviceVersion,
               resource = NymphHandler.res,
               port = args.port,
               host = args.host)
    Thread.currentThread().join()
  }
}

object NymphHandler {
  object res extends Resource {

    private val allowedMethods: Set[String] = Set(
      "register",
      "queryAccount",
      "sendTransaction",
      "queryTransactionStatus"
    )

    /** if method included in current resource
      *
      */
    override def contains(method: String): Boolean = allowedMethods contains method

    /** invoke method with parameters */
    override def invoke(method: String, params: Json): Either[Throwable, Json] = method match {
      case "register" => Right(Json.fromString("demo"))
      case x          => Left(new UnsupportedOperationException(s"unsupported operation: $x"))

    }

    /** can the params be accepted
      *
      */
    override def paramsAcceptable(method: String, params: Json): Boolean = method match {
      case "register" => params.asString.isDefined // def register(rand: String): SP[F, Account]

      case _ => true
    }
  }
}
