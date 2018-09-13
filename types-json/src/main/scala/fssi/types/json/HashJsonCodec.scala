package fssi
package types
package json

import types._,implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait HashJsonCodec {
  implicit val hashJsonEncoder: Encoder[Hash] = (a: Hash) => a.value.asJson
  implicit val hashJsonDecoder: Decoder[Hash] = (h: HCursor) => {
    for {
      value <- h.as[HexString]
    } yield Hash(value)
  }
}
