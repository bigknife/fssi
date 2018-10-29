package fssi
package types
package json
import fssi.types.base.{BytesValue, Hash}
import io.circe._
import types.implicits._

trait BaseHashJsonCodec {

  implicit val hashEncoder: Encoder[Hash] = Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val hashDecoder: Decoder[Hash] =
    Decoder[String].map(x => Hash(BytesValue.decodeBcBase58(x).get.bytes))
}
