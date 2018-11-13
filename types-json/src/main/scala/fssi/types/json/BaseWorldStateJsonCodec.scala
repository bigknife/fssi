package fssi
package types
package json

import fssi.base.BytesValue
import types.base._
import types.implicits._
import io.circe._

trait BaseWorldStateJsonCodec {
  implicit val worldStateJsonEncoder: Encoder[HashState] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val worldStateJsonDecoder: Decoder[HashState] =
    Decoder[String].map { x =>
      HashState(BytesValue.decodeBcBase58(x).get.bytes)
    }
}
