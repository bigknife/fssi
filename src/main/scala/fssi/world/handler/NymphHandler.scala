package fssi.world.handler

import fssi.world.Args.NymphArgs
import bigknife.jsonrpc._
import bigknife.jsonrpc.implicits._
import io.circe.Json
import fssi.ast.domain.components.Model.Op
import fssi.ast.usecase.Nymph
import fssi.interpreter._
import scala.util._

class NymphHandler extends ArgsHandler[NymphArgs] {

  override def run(args: NymphArgs): Unit = {
    server.run(name = args.serviceName,
               version = args.serviceVersion,
               resource = NymphHandler.res(args),
               port = args.port,
               host = args.host)
    Thread.currentThread().join()
  }
}

object NymphHandler {

  import io.circe.syntax._
  import io.circe.generic.semiauto._
  import jsonCodec._

  val nymph: Nymph[Op] = Nymph[Op]

  case class res(args: NymphArgs) extends Resource {

    lazy val setting: Setting = Setting() // from args

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
      case "register" => invokeRegister(params)
      case x          => Left(new UnsupportedOperationException(s"unsupported operation: $x"))

    }

    /** can the params be accepted
      *
      */
    override def paramsAcceptable(method: String, params: Json): Boolean = method match {
      case "register" => params.asString.isDefined // def register(rand: String): SP[F, Account]

      case _ => true
    }

    private def invokeRegister(params: Json): Either[Throwable, Json] = {
      Try {
        runner.runIOAttempt(nymph.register(params.asString.get), setting).unsafeRunSync()
      }.toEither match {
        case Left(x)               => Left(x): Either[Throwable, Json]
        case Right(Left(x))        => Left(x): Either[Throwable, Json]
        case Right(Right(account)) => Right(account.asJson): Either[Throwable, Json]
      }
    }
  }
}
