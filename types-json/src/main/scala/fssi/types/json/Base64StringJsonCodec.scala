package fssi
package types
package json

import types._
import utils._
import io.circe._
import implicits._

trait Base64StringJsonCodec {
  implicit val base64JsonEncoder: Encoder[Base64String] = (a: Base64String) => Json.fromString(a.toString)
  implicit val base64JsonDecoder: Decoder[Base64String] = (h: HCursor) => {
    for {
      base64 <- h.as[String]
    } yield Base64String(BytesUtil.decodeBase64(base64))
  }
}
