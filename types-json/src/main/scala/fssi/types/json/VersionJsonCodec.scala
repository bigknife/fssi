package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait VersionJsonCodec {
  implicit val versionJsonEncoder: Encoder[Version] = (a: Version) => Json.fromString(a.value)
  implicit val versionJsonDecoder: Decoder[Version] = (h: HCursor) => {
    for {
      value <- h.as[String]
    } yield Version(value)
  }
}
