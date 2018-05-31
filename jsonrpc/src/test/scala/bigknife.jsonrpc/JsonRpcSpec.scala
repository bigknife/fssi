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
  }

  "POST -> /jsonrpc/test/v1" should "got a jsonrpc output" in {
    val request = IO {
      org.http4s
        .Request[IO](Method.POST, Uri.unsafeFromString("/jsonrpc/test/v1"))
        .withBody("""
            |{
            |	"jsonrpc": "2.0",
            |	"method": "test",
            |	"params": ["abcdefg", "bcd"],
            |	"id": "1"
            |}
          """.stripMargin)
    }

    //val responseIO = Server.jsonrpcService("test", "v1", testResource).run(request)

    //println(responseIO)
  }
}
