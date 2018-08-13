package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait TransactionJsonCodec {
  import Transaction._

  implicit val transactionIDJsonEncoder: Encoder[Transaction.ID] = (a: Transaction.ID) =>
    Json.fromString(a.value)

  implicit val transactionIDJsonDecoder: Decoder[Transaction.ID] = (h: HCursor) => {
    for {
      value <- h.as[String]
    } yield Transaction.ID(value)
  }

  implicit val transferJsonEncoder: Encoder[Transfer] = (a: Transfer) =>
    Json.obj(
      "id"        -> a.id.asJson,
      "from"      -> a.from.asJson,
      "to"        -> a.to.asJson,
      "token"     -> a.token.asJson,
      "signature" -> a.signature.asJson,
      "timestamp" -> a.timestamp.asJson
  )

  implicit val publishContractJsonEncoder: Encoder[PublishContract] = (a: PublishContract) =>
    Json.obj(
      "id"        -> a.id.asJson,
      "owner"     -> a.owner.asJson,
      "contract"  -> a.contract.asJson,
      "signature" -> a.signature.asJson,
      "timestamp" -> a.timestamp.asJson
  )

  implicit val runContractJsonEncoder: Encoder[RunContract] = (a: RunContract) =>
    Json.obj(
      "id"                -> a.id.asJson,
      "sender"            -> a.sender.asJson,
      "contractName"      -> a.contractName.asJson,
      "contractVersion"   -> a.contractVersion.asJson,
      "contractMethod"    -> a.contractMethod.asJson,
      "contractParameter" -> a.contractParameter.asJson,
      "signature"         -> a.signature.asJson,
      "timestamp"         -> a.timestamp.asJson
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

  implicit val transactionJsonDecoder: Decoder[Transaction] = ???
}
