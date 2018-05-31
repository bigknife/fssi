package bigknife.jsonrpc

import cats.effect.IO
import org.scalatest._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.server._
import org.http4s.dsl._

class JsonRpcSpec extends FlatSpec with Matchers {

  object testResource extends Resource {

    /** if method included in current resource
      *
      */
    override def contains(method: String): Boolean = method == "test1" || method == "test2"

    /** invoke method with parameters */
    override def invoke(method: String, params: Json): Either[Throwable, Json] = {
      if (method == "test1") Right(Json.fromString("test1 ok"))
      else Left(new Exception("test2 exception"))
    }

    /** can the params be accepted
      *
      */
    override def paramsAcceptable(method: String, params: Json): Boolean = true
  }

  "POST -> /jsonrpc/test/v1" should "got a jsonrpc output" in {
    val req1 =
      org.http4s
        .Request[IO](Method.POST, Uri.unsafeFromString("/jsonrpc/test/v1"))
        .withBody("""
            |{
            |	"jsonrpc": "2.0",
            |	"method": "test1",
            |	"params": ["abcdefg", "bcd"],
            |	"id": "1"
            |}
          """.stripMargin)
        .unsafeRunSync()

    val service: org.http4s.HttpService[IO] = Server.jsonrpcService("test", "v1", testResource)
    val json1                               = service.run(req1).value.unsafeRunSync().get.as[Json].unsafeRunSync()
    assert(json1.noSpaces == "{\"id\":\"1\",\"jsonrpc\":\"2.0\",\"result\":\"test1 ok\"}")
    info(json1.noSpaces)

    val req2 =
      org.http4s
        .Request[IO](Method.POST, Uri.unsafeFromString("/jsonrpc/test/v1"))
        .withBody("""
                    |{
                    |	"jsonrpc": "2.0",
                    |	"method": "test2",
                    |	"params": ["abcdefg", "bcd"],
                    |	"id": "1"
                    |}
                  """.stripMargin)
        .unsafeRunSync()

    val json2 = service.run(req2).value.unsafeRunSync().get.as[Json].unsafeRunSync()
    assert(
      json2.noSpaces == "{\"jsonrpc\":\"2.0\",\"error\":{\"data\":\"test2 exception\"," +
        "\"code\":-32603,\"message\":\"Internal error\"}}")

    info(json2.noSpaces)

  }
}
