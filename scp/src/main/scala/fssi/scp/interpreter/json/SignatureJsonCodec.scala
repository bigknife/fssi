package fssi
package scp
package interpreter
package json

import types._
import io.circe._
import fssi.base.implicits._
import fssi.base._
import fssi.scp.types.implicits._

trait SignatureJsonCodec {

  implicit val signatureEncoder: Encoder[Signature] =
    Encoder[String].contramap(x => x.asBytesValue.bcBase58)

  implicit val signatureDecoder: Decoder[Signature] =
    Decoder[String].map(x => Signature(BytesValue.unsafeDecodeBcBase58(x).bytes))
}
