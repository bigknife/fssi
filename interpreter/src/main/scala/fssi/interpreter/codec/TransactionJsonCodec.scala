package fssi.interpreter.codec

import fssi.ast.domain.types.Transaction
import fssi.ast.domain.types.Transaction.Transfer
import io.circe.{Encoder, Json}
import TokenJsonCodec._
import io.circe.syntax._

trait TransactionJsonCodec {
  implicit val transactionJsonEncoder: Encoder[Transaction] = {
    case t: Transfer =>
      Json.obj(
        "type" -> Json.fromString("Transaction.Transfer"),
        "impl" -> transferJsonEncoder(t)
      )

    case _ => ???
  }
  implicit val transferJsonEncoder: Encoder[Transfer] = (t: Transfer) =>
    Json.obj(
      "id" -> Json.fromString(t.id.value),
      "from" -> Json.fromString(t.from.value),
      "to" -> Json.fromString(t.to.value),
      "amount" -> t.amount.asJson,
      "signature" -> Json.fromString(t.signature.hex),
      "status" -> Json.fromString(t.status.toString)
  )
}
