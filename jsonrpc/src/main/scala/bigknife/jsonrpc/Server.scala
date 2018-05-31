package bigknife.jsonrpc

import io.circe.{Json, JsonObject}

trait Server {
  //import cats.implicits._
  import cats.effect._
  import org.http4s.server.blaze._
  //import org.http4s.implicits._
  scala.concurrent.ExecutionContext.Implicits.global

  def run(name: String,
          version: String,
          resource: Resource,
          port: Int = 8080,
          host: String = "0.0.0.0"): Unit = {
    val service = Server.jsonrpcService(name, version, resource)

    val builder = BlazeBuilder[IO]
      .bindHttp(port, host)
      .mountService(service, "/")
      .start

    val server = builder.unsafeRunSync()
    // add shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread(() => server.shutdown.unsafeRunSync()))
  }
}

object Server {
  import cats.effect._
  import org.http4s._
  import org.http4s.dsl.io._
  import org.http4s.circe._
  import io.circe.syntax._
  import bigknife.jsonrpc.implicits._
  import bigknife.jsonrpc.Response._

  def jsonrpcService(name: String, version: String, resource: Resource): HttpService[IO] =
    HttpService[IO] {
      case req @ POST -> Root / "jsonrpc" / `name` / `version` =>
        type Resp = IO[org.http4s.Response[IO]]
        def parseJson(next: Json => Resp): Resp =
          for {
            bodyOr <- req.as[Json].attempt
            resp <- bodyOr match {
              case Left(t) =>
                val response: bigknife.jsonrpc.Response[String] =
                  invalidRequest[String](Some(t.getMessage))
                BadRequest(response.asJson)
              case Right(json) => next(json)
            }
          } yield resp

        def validateJson(json: Json)(next: bigknife.jsonrpc.Request[Json] => Resp): Resp = {
          // json should be object
          // MUST Container 4 fields
          // version should be 2.0
          json.asObject match {
            case Some(jso) =>
              val requireKey = jso.contains("id") && jso.contains("jsonrpc") && jso
                .contains("method") && jso.contains("params")
              if (!requireKey)
                BadRequest(
                  invalidRequest(Some(
                    "jsonrpc request object should contain id, jsonrpc, method and params")).asJson)
              else if (!jso("jsonrpc").contains(Json.fromString(Version)))
                BadRequest(invalidRequest(Some("jsonrpc should be 2.0")).asJson)
              else {
                val id      = jso("id").get.asString.get
                val method  = jso("method").get.asString.get
                val params  = jso("params").get
                val request = bigknife.jsonrpc.Request[Json](id, method, params)
                next(request)
              }

            case None =>
              BadRequest(invalidRequest(Some("jsonrpc request should be a json object")).asJson)
          }
        }

        def validateParams(req: bigknife.jsonrpc.Request[Json])(
            next: bigknife.jsonrpc.Request[Json] => Resp): Resp = {
          if (resource.paramsAcceptable(req.method, req.params)) next(req)
          else BadRequest(invalidParams(Some("Parameters can't be accepted.")).asJson)
        }

        def invokeMethod(req: bigknife.jsonrpc.Request[Json]): Resp = {
          if (resource.contains(req.method)) {
            resource.invoke(req.method, req.params) match {
              case Left(t) => InternalServerError(internalError(Some(t.getMessage)).asJson)
              case Right(x) =>
                Ok(success(req.id, x).asJson)
            }
          } else NotFound(methodNotFound(Some(s"Method Not Found: ${req.method}")).asJson)

        }

        parseJson { json =>
          validateJson(json) { req =>
            validateParams(req) { validReq =>
              invokeMethod(validReq)
            }

          }
        }
    }
}
