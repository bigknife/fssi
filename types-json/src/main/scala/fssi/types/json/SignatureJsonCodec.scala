package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait SignatureJsonCodec {
  implicit val signatureJsonEncoder: Encoder[Signature] = (a: Signature) => a.value.asJson
  implicit val signatureJsonDecoder: Decoder[Signature] = (h: HCursor) => {
    for {
      value <- h.as[HexString]
    } yield Signature(value)
  }
}
