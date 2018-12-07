package fssi
package types
package json
import fssi.base.BytesValue
import fssi.types.base.Hash
import io.circe._
import types.implicits._

trait BaseHashJsonCodec {

  implicit val hashEncoder: Encoder[Hash] = Encoder[String].contramap(_.asBytesValue.base64)

  implicit val hashDecoder: Decoder[Hash] =
    Decoder[String].map(x => Hash(BytesValue.unsafeDecodeBase64(x).bytes))
}
