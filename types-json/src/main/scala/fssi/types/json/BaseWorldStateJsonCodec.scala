package fssi
package types
package json

import fssi.base.BytesValue
import types.base._
import types.implicits._
import io.circe._

trait BaseWorldStateJsonCodec {
  implicit val worldStateJsonEncoder: Encoder[WorldState] =
    Encoder[String].contramap(_.asBytesValue.base64)

  implicit val worldStateJsonDecoder: Decoder[WorldState] =
    Decoder[String].map { x =>
      WorldState(BytesValue.unsafeDecodeBase64(x).bytes)
    }
}
