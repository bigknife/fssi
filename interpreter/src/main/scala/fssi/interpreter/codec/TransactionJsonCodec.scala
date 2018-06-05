package fssi.interpreter.codec

import fssi.ast.domain.types._
import fssi.ast.domain.types.Transaction.Transfer
import io.circe._
import TokenJsonCodec._
import io.circe.Decoder.Result
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

  implicit val transactionJsonDecoder: Decoder[Transaction] = (c: HCursor) => {
    c.get[String]("type") match {
      case Left(t) => Left(t): Result[Transaction]
      case Right("Transaction.Transfer") =>
        c.get[Json]("impl") match {
          case Left(t)     => Left(t): Result[Transaction]
          case Right(json) => json.as[Transfer]
        }

      case Right(_) => ???
    }
  }

  implicit val transferJsonEncoder: Encoder[Transfer] = (t: Transfer) =>
    Json.obj(
      "id"        -> Json.fromString(t.id.value),
      "from"      -> Json.fromString(t.from.value),
      "to"        -> Json.fromString(t.to.value),
      "amount"    -> t.amount.asJson,
      "signature" -> Json.fromString(t.signature.hex),
      "status"    -> t.status.asJson
  )

  implicit val transactionStatusEncoder: Encoder[Transaction.Status] = {
    case Transaction.Status.Init(Transaction.ID(value))     => Json.fromString(s"Init:$value")
    case Transaction.Status.Pending(Transaction.ID(value))  => Json.fromString(s"Pending:$value")
    case Transaction.Status.Rejected(Transaction.ID(value)) => Json.fromString(s"Rejected:$value")
  }

  implicit val transactionStatusDecoder: Decoder[Transaction.Status] = (c: HCursor) => {
    c.as[String].right.flatMap { x =>
      x.split(":") match {
        case Array("Init", id)     => Right(Transaction.Status.Init(Transaction.ID(id)))
        case Array("Pending", id)  => Right(Transaction.Status.Pending(Transaction.ID(id)))
        case Array("Rejected", id) => Right(Transaction.Status.Rejected(Transaction.ID(id)))
        case x                     => Left(DecodingFailure(s"unsupported status: $x", List(CursorOp.MoveFirst)))
      }
    }
  }

  implicit val transferJsonDecoder: Decoder[Transfer] = (c: HCursor) => {
    for {
      id        <- c.get[String]("id")
      from      <- c.get[String]("from")
      to        <- c.get[String]("to")
      amount    <- c.get[Token]("amount")
      signature <- c.get[String]("signature")
      status    <- c.get[Transaction.Status]("status")
    } yield
      Transfer(
        Transaction.ID(id),
        Account.ID(from),
        Account.ID(to),
        amount,
        Signature(BytesValue.decodeHex(signature)),
        status
      )
  }
}

object TransactionJsonCodec extends TransactionJsonCodec
