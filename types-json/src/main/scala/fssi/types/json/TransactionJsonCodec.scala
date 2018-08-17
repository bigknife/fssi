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
      "payer"      -> a.payer.asJson,
      "payee"        -> a.payee.asJson,
      "token"     -> a.token.asJson,
      "signature" -> a.signature.asJson,
      "timestamp" -> a.timestamp.asJson
  )

  implicit val transferJsonDecoder: Decoder[Transfer] = (a: HCursor) => {
    for {
      id        <- a.get[Transaction.ID]("id")
      payer      <- a.get[Account.ID]("payer")
      payee        <- a.get[Account.ID]("payee")
      token     <- a.get[Token]("token")
      signature <- a.get[Signature]("signature")
      timestamp <- a.get[Long]("timestamp")
    } yield Transfer(id, payer, payee, token, signature, timestamp)
  }

  implicit val publishContractJsonEncoder: Encoder[PublishContract] = (a: PublishContract) =>
    Json.obj(
      "id"        -> a.id.asJson,
      "owner"     -> a.owner.asJson,
      "contract"  -> a.contract.asJson,
      "signature" -> a.signature.asJson,
      "timestamp" -> a.timestamp.asJson
  )

  implicit val publishContractJsonDecoder: Decoder[PublishContract] = (a: HCursor) => {
    for {
      id        <- a.get[Transaction.ID]("id")
      owner     <- a.get[Account.ID]("owner")
      contract  <- a.get[Contract.UserContract]("contract")
      signature <- a.get[Signature]("signature")
      timestamp <- a.get[Long]("timestamp")
    } yield PublishContract(id, owner, contract, signature, timestamp)
  }

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

  implicit val runContractJsonDecoder: Decoder[RunContract] = (a: HCursor) => {
    for {
      id                <- a.get[Transaction.ID]("id")
      sender            <- a.get[Account.ID]("sender")
      contractName      <- a.get[UniqueName]("contractName")
      contractVersion   <- a.get[Version]("contractVersion")
      contractMethod    <- a.get[Contract.Method]("contractMethod")
      contractParameter <- a.get[Contract.Parameter]("contractParameter")
      signature         <- a.get[Signature]("signature")
      timestamp         <- a.get[Long]("timestamp")
    } yield
      RunContract(id,
                  sender,
                  contractName,
                  contractVersion,
                  contractMethod,
                  contractParameter,
                  signature,
                  timestamp)
  }

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

  implicit val transactionJsonDecoder: Decoder[Transaction] = (a: HCursor) => {
    a.get[String]("type") match {
      case Right("Transfer")        => a.get[Transaction.Transfer]("transaction")
      case Right("Publishcontract") => a.get[Transaction.PublishContract]("transaction")
      case Right("RunContract")     => a.get[Transaction.RunContract]("transaction")
      case x =>
        Left(
          DecodingFailure(
            "transaction json require 'type' field, and should be 'Transfer','PublishContract' or 'RunContract'",
            List(CursorOp.DownField("type"))))
    }
  }

}
