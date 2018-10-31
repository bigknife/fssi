package fssi
package scp
package interpreter
package json

import types._
import io.circe._
import base.implicits._
import fssi.base.BytesValue
import types.implicits._

trait NodeIDJsonCodec {

  implicit val nodeIDEncoder: Encoder[NodeID] =
    Encoder[String].contramap(x => x.asBytesValue.bcBase58)

  implicit val nodeIDDecoder: Decoder[NodeID] =
    Decoder[String].map(x => NodeID(BytesValue.unsafeDecodeBcBase58(x).bytes))
}
