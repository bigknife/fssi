package fssi
package types
package json

import types._
import utils._
import io.circe._
import implicits._

trait HexStringJsonCodec {
  implicit val hexStringJsonEncoder: Encoder[HexString] = (a: HexString) => Json.fromString(a.toString)
  implicit val hexStringJsonDecoder: Decoder[HexString] = (h: HCursor) => {
    for {
      hex <- h.as[String]
    } yield HexString.decode(hex)
  }
}
