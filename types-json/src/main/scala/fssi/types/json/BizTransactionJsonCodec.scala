package fssi
package types
package json

import fssi.types.base.BytesValue
import fssi.types.biz._
import fssi.types.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._

trait BizTransactionJsonCodec {
  implicit val bizTransactionIdEncoder: Encoder[Transaction.ID] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val bizTransactionIdDecoder: Decoder[Transaction.ID] =
    Decoder[String].map(x => Transaction.ID(BytesValue.decodeBcBase58(x).get.bytes))

  implicit val bizTransactionEncoder: Encoder[Transaction] = {
    case x: Transaction.Transfer =>
      Json.obj("type" -> Json.fromString("Transfer"),
               "data" -> Encoder[Transaction.Transfer].apply(x))
    case x: Transaction.Deploy =>
      Json.obj("type" -> Json.fromString("Deploy"), "data" -> Encoder[Transaction.Deploy].apply(x))
    case x: Transaction.Run =>
      Json.obj("type" -> Json.fromString("Run"), "data" -> Encoder[Transaction.Run].apply(x))
  }

  implicit val bizTransactionDecoder: Decoder[Transaction] = (hCursor: HCursor) =>
    hCursor.get[String]("type") match {
      case Right(t) =>
        t match {
          case tf if tf == "Transfer" => hCursor.get[Transaction.Transfer]("data")
          case d if d == "Deploy"     => hCursor.get[Transaction.Deploy]("data")
          case r if r == "Run"        => hCursor.get[Transaction.Run]("data")
          case x =>
            Left(
              DecodingFailure(
                s"type of transaction should be Transfer | Deploy | Run , but found $x",
                List()))
        }
      case Left(e) => Left(e)
  }
}
