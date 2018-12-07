package fssi
package types
package json

import fssi.base.BytesValue
import fssi.types.base._
import fssi.types.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._

trait BaseUniqueNameJsonCodec {
  implicit val baseUniqueNameEncoder: Encoder[UniqueName] =
    Encoder[String].contramap(_.asBytesValue.base64)

  implicit val baseUniqueNameDecoder: Decoder[UniqueName] =
    Decoder[String].map(x => UniqueName(BytesValue.unsafeDecodeBase64(x).bytes))
}
