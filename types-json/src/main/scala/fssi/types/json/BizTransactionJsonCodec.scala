package fssi
package types
package json
import fssi.base.BytesValue
import fssi.types.biz._
import fssi.types.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._

trait BizTransactionJsonCodec {
  implicit val bizTransactionIdEncoder: Encoder[Transaction.ID] =
    Encoder[String].contramap(_.asBytesValue.base64)

  implicit val bizTransactionIdDecoder: Decoder[Transaction.ID] =
    Decoder[String].map(x => Transaction.ID(BytesValue.unsafeDecodeBase64(x).bytes))

  implicit val bizTransactionEncoder: Encoder[Transaction] = {
    case x: Transaction.Transfer =>
      Json.obj("type"        -> Json.fromString("Transfer"),
               "transaction" -> Encoder[Transaction.Transfer].apply(x))
    case x: Transaction.Deploy =>
      Json.obj("type"        -> Json.fromString("Deploy"),
               "transaction" -> Encoder[Transaction.Deploy].apply(x))
    case x: Transaction.Run =>
      Json.obj("type" -> Json.fromString("Run"), "transaction" -> Encoder[Transaction.Run].apply(x))
  }

  implicit val bizTransactionDecoder: Decoder[Transaction] = (hCursor: HCursor) =>
    hCursor.get[String]("type") match {
      case Right(t) =>
        t match {
          case tf if tf == "Transfer" => hCursor.get[Transaction.Transfer]("transaction")
          case d if d == "Deploy"     => hCursor.get[Transaction.Deploy]("transaction")
          case r if r == "Run"        => hCursor.get[Transaction.Run]("transaction")
          case x =>
            Left(
              DecodingFailure(
                s"type of transaction should be Transfer | Deploy | Run , but found $x",
                List()))
        }
      case Left(e) => Left(e)
  }

  implicit val transactionSetEncoder: Encoder[TransactionSet] = (transactionSet: TransactionSet) =>
    Json.arr(transactionSet.map(_.asJson).toSeq: _*)

  implicit val transactionSetDecoder: Decoder[TransactionSet] = (h: HCursor) =>
    h.values match {
      case Some(jsons) =>
        val rs = jsons.foldLeft(TransactionSet.empty) { (acc, n) =>
          n.as[Transaction] match {
            case Right(value) => acc + value
            case Left(e)      => throw DecodingFailure(e.getMessage(), List(CursorOp.DownArray))
          }
        }
        Right(rs)
      case None => Left(DecodingFailure("", List(CursorOp.DownArray)))
  }
}
