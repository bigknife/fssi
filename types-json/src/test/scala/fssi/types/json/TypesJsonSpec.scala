package fssi
package types
package json

import org.scalatest._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import fssi.types.base._
import fssi.types.biz._
import json.implicits._

class TypesJsonSpec extends FunSuite {
  test("WorldState json encoder and decoder") {
    val worldState = HashState("Hello,world".getBytes)
    val json = worldState.asJson.spaces2
    info(s"$json")

    val worldState1 = for {
      j <- parse(json)
      x <- j.as[HashState]
    } yield x

    assert(worldState1.isRight)
    assert(worldState1.right.get === worldState)
  }

}
