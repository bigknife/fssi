package fssi
package scp
package interpreter
package json

import types._
import utils._
import io.circe._

trait SignatureJsonCodec {

  implicit val signatureEncoder: Encoder[Signature] =
    Encoder[String].contramap(x => BytesUtil.toBase64(x.value))

  implicit val signatureDecoder: Decoder[Signature] =
    Decoder[String].map(x => Signature(BytesUtil.decodeBase64(x)))
}
