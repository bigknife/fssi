package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait TransactionJsonCodec {
  import Transaction._

  implicit val transferJsonEncoder: Encoder[Transfer] = (a: Transfer) =>
  Json.obj(
    "id" -> Json.fromString(a.id.value),
    "from" -> Json.fromString(a.from.value.toString),
    "to" -> Json.fromString(a.to.value.toString),
    )
  implicit val publishContractJsonEncoder: Encoder[PublishContract] = (a: PublishContract) =>
    Json.obj(
    )
  implicit val runContractJsonEncoder: Encoder[RunContract] = (a: RunContract) =>
    Json.obj(
    )

  implicit val transactionJsonEncoder: Encoder[Transaction] = (a: Transaction) =>
    a match {
      case x: Transfer =>
        Json.obj(
          "type"        -> Json.fromString("Transfer"),
          "transaction" -> x.asJson
        )
      case x: PublishContract =>
        Json.obj(
          "type"        -> Json.fromString("PublishContract"),
          "transaction" -> x.asJson
        )
      case x: RunContract =>
        Json.obj(
          "type"        -> Json.fromString("RunContract"),
          "transaction" -> x.asJson
        )
  }
}
