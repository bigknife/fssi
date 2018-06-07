package fssi.world.handler

import fssi.world.Args.NymphArgs
import bigknife.jsonrpc._
import io.circe.{DecodingFailure, Json}
import fssi.ast.domain.components.Model.Op
import fssi.ast.domain.types.Transaction
import fssi.ast.usecase.Nymph
import fssi.interpreter._
import org.slf4j.{Logger, LoggerFactory}

import scala.util._

class NymphHandler extends ArgsHandler[NymphArgs] {

  override def logbackConfigResource(args: NymphArgs): String =
    (args.verbose, args.colorfulLog) match {
      case (true, true)   => "/logback-nymph-verbose-color.xml"
      case (true, false)  => "/logback-nymph-verbose.xml"
      case (false, false) => "/logback-nymph.xml"
      case (false, true)  => "/logback-nymph-color.xml"
    }

  override def run(args: NymphArgs): Unit = {

    // first start a p2p node
    NymphHandler.startP2PNode(args)

    // run jsonrpc server
    NymphHandler.startJsonrpcServer(args)

    Thread.currentThread().join()
  }
}

object NymphHandler {

  import io.circe.syntax._
  import jsonCodec._

  val nymph: Nymph[Op] = Nymph[Op]

  val logger: Logger        = LoggerFactory.getLogger(getClass)
  val jsonrpcLogger: Logger = LoggerFactory.getLogger("fssi.jsonrpc")

  def startJsonrpcServer(args: NymphArgs): Unit = {
    Try {
      server.run(name = args.jsonrpcServiceName,
                 version = args.jsonrpcServiceVersion,
                 resource = NymphHandler.res(args),
                 port = args.jsonrpcPort,
                 host = args.jsonrpcHost)
    } match {
      case Success(_) =>
        logger.info(
          s"nymph jsonrpc server started, listening on ${args.jsonrpcHost}:${args.jsonrpcPort}")
      case Failure(t) => logger.error("nymph jsonrpc server start failed", t)
    }

  }

  // start p2p node
  def startP2PNode(args: NymphArgs): Unit = {
    val p = nymph.startup(args.nodeIp, args.nodePort, args.seeds)
    runner.runIOAttempt(p, args.toSetting).unsafeRunSync() match {
      case Left(t)  => logger.error("start p2p node failed", t)
      case Right(v) => logger.info("p2p node start, id {}", v.toString)
    }
    // add shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      runner.runIOAttempt(nymph.shutdown(), args.toSetting).unsafeRunSync() match {
        case Left(t)  => logger.warn("shutdown p2p node failed", t)
        case Right(_) => logger.debug("p2p node shut down.")
      }
    }))
  }

  // jsonrpc resources
  case class res(args: NymphArgs) extends Resource {

    lazy val setting: Setting = args.toSetting

    private val allowedMethods: Set[String] = Set(
      "register",
      "queryAccount",
      "sendTransaction",
      "queryTransactionStatus"
    )

    jsonrpcLogger.debug("jsonrpc supported method: ")
    jsonrpcLogger.debug("==========================")
    allowedMethods foreach { x =>
      jsonrpcLogger.debug(s"    $x")
    }
    jsonrpcLogger.debug("==========================")

    /** if method included in current resource
      *
      */
    override def contains(method: String): Boolean = allowedMethods contains method

    /** invoke method with parameters */
    override def invoke(method: String, params: Json): Either[Throwable, Json] = {
      jsonrpcLogger.debug(s"invoking $method with params = ${params.spaces2}")
      val res = method match {
        case "register"        => invokeRegister(params)
        case "sendTransaction" => invokeSendTransaction(params)
        case x                 => Left(new UnsupportedOperationException(s"unsupported operation: $x"))

      }
      res.left
        .map { t =>
          jsonrpcLogger.error(s"invoking $method failed", t); t
        }
        .right
        .map { json =>
          jsonrpcLogger.info(s"invoked $method, got result: ${json.spaces2}"); json
        }
    }

    /** can the params be accepted
      *
      */
    override def paramsAcceptable(method: String, params: Json): Boolean = method match {
      case "register"        => params.asString.isDefined // def register(rand: String): SP[F, Account]
      case "sendTransaction" => true //todo: check the format

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

    private def invokeSendTransaction(params: Json): Either[Throwable, Json] = {
      Try {
        params.as[Transaction].right.map { transaction =>
          runner
            .runIOAttempt(nymph.sendTransaction(transaction.sender, transaction), setting)
            .unsafeRunSync()
        }
      }.toEither match {
        case Left(x)                    => Left(x): Either[Throwable, Json]
        case Right(Left(x))             => Left(x): Either[Throwable, Json]
        case Right(Right(Left(x)))      => Left(x): Either[Throwable, Json]
        case Right(Right(Right(value))) => Right(value.asJson): Either[Throwable, Json]
      }
    }
  }
}
