package fssi
package types
package json

import types.base._
import types.implicits._
import io.circe._

trait BaseWorldStateJsonCodec {
  implicit val worldStateJsonEncoder: Encoder[WorldState] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val worldStateJsonDecoder: Decoder[WorldState] =
    Decoder[String].map {x =>
      WorldState(BytesValue.decodeBcBase58(x).get.bytes)
    }
}
