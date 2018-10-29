package fssi
package types
package json
import org.scalatest.FunSuite

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import json.implicits._

class JsonMessageJsonCodecSpec extends FunSuite {

  test("test json message coder") {
    val jsonMessage = JsonMessage(JsonMessage.TYPE_NAME_TRANSACTION, "send transaction")

    val jsonStr = jsonMessage.asJson.spaces2
    info(jsonStr)

    val result = for {
      json <- parse(jsonStr)
      r    <- json.as[JsonMessage]
    } yield r

    assert(result.isRight)
    assert(result.right.get == jsonMessage)
  }
}
