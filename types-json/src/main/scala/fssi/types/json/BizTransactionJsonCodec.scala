package fssi
package types
package json

import fssi.types.biz._
import fssi.types.implicits._

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._

trait BizTransactionJsonCodec {
  implicit val bizTransactionIdEncoder: Encoder[Transaction.ID] = Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val bizTransactionEncoder: Encoder[Transaction] = {
    case x: Transaction.Transfer => Json.obj("Transfer" -> Encoder[Transaction.Transfer].apply(x))
  }
}
