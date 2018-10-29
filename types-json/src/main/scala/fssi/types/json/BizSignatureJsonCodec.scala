package fssi
package types
package json

import fssi.types.base._
import fssi.types.implicits._

import io.circe._
import io.circe.syntax._

trait BizSignatureJsonCodec {
  implicit val bizSignatureId: Encoder[Signature] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val bizSignatureDecoder: Decoder[Signature] =
    Decoder[String].map(x => Signature(BytesValue.decodeBcBase58(x).get.bytes))
}
