package fssi.interpreter.codec

import fssi.ast.domain.types.TransactionSendingStatus
import io.circe.{Encoder, Json}
import io.circe.syntax._

import fssi.interpreter.codec.TransactionJsonCodec._

trait TransactionSendingStatusCodec {
  implicit val transactionSendingStatusJsonEncoder: Encoder[TransactionSendingStatus] =
    (s: TransactionSendingStatus) =>
      Json.obj(
        "tip"    -> Json.fromString(s.tip),
        "status" -> s.status.asJson
    )
}

object TransactionSendingStatusCodec extends TransactionSendingStatusCodec
