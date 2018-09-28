package fssi
package types
package json

import types.base._
import types.implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait BaseWorldStateJsonCodec {
  implicit val worldStateJsonEncoder: Encoder[WorldState] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)
}
