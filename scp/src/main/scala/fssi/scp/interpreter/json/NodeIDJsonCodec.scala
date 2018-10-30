package fssi
package scp
package interpreter
package json

import types._
import utils._
import io.circe._

trait NodeIDJsonCodec {

  implicit val nodeIDEncoder: Encoder[NodeID] =
    Encoder[String].contramap(x => BytesUtil.toBase64(x.value))

  implicit val nodeIDDecoder: Decoder[NodeID] =
    Decoder[String].map(x => NodeID(BytesUtil.decodeBase64(x)))
}
