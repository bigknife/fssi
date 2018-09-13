package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait UniqueNameJsonCodec {
  implicit val uniqueNameJsonEncoder: Encoder[UniqueName] = (a: UniqueName) => Json.fromString(a.value)
  implicit val uniqueNameJsonDecoder: Decoder[UniqueName] = (h: HCursor) => {
    for {
      value <- h.as[String]
    } yield UniqueName(value)
  }
}
