package fssi
package types
package json

import types._
import io.circe._

trait JsonMessageCodec {
  implicit val jsonMessageEncoder: Encoder[JsonMessage] = (a: JsonMessage) => {
    Json.obj(
      "typeName" -> Json.fromString(a.typeName),
      "body"     -> Json.fromString(a.body)
    )
  }

  implicit val jsonMessageDecoder: Decoder[JsonMessage] = (c: HCursor) => {
    for {
      typeName <- c.get[String]("typeName")
      body     <- c.get[String]("body")
    } yield JsonMessage(typeName, body)
  }
}
