package fssi
package types
package json

import fssi.types.biz._
import fssi.types.implicits._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._

trait BizContractJsonCodec {
  implicit val userContractCodeEncoder: Encoder[Contract.UserContract.Code] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val bizVersionEncoder: Encoder[Contract.Version] =
    Encoder[String].contramap(_.toString)
}
