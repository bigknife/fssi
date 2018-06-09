package fssi.interpreter.codec

import fssi.ast.domain.types._
import fssi.ast.domain.types.Transaction.{InvokeContract, PublishContract, Transfer}
import io.circe._
import TokenJsonCodec._
import io.circe.Decoder.Result
import io.circe.syntax._
import ContractJsonCodec._

trait TransactionJsonCodec {
  val TransactionType_Transfer: String    = "Transaction.Transfer"
  val TransactionType_PubContract: String = "Transaction.PublishContract"
  val TransactionType_InvContract: String = "Transaction.InvokeContract"

  implicit val transferJsonEncoder: Encoder[Transfer] = (t: Transfer) =>
    Json.obj(
      "id"        -> Json.fromString(t.id.value),
      "from"      -> Json.fromString(t.from.value),
      "to"        -> Json.fromString(t.to.value),
      "amount"    -> t.amount.asJson,
      "signature" -> Json.fromString(t.signature.base64),
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
        case x0                    => Left(DecodingFailure(s"unsupported status: $x0", List(CursorOp.MoveFirst)))
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
        Signature(BytesValue.decodeBase64(signature)),
        status
      )
  }

  implicit val publishContractJsonEncoder: Encoder[PublishContract] = (p: PublishContract) => {
    Json.obj(
      "id"        -> Json.fromString(p.id.value),
      "owner"     -> Json.fromString(p.owner.value),
      "contract"  -> p.contract.asJson,
      "signature" -> Json.fromString(p.signature.base64),
      "status"    -> p.status.asJson
    )
  }

  implicit val publishContractJsonDecoder: Decoder[PublishContract] = (c: HCursor) => {
    for {
      id        <- c.get[String]("id")
      owner     <- c.get[String]("owner")
      contract  <- c.get[Contract.UserContract]("contract")
      signature <- c.get[String]("signature")
      status    <- c.get[Transaction.Status]("status")
    } yield
      PublishContract(
        Transaction.ID(id),
        Account.ID(owner),
        contract,
        Signature(BytesValue.decodeBase64(signature)),
        status
      )
  }

  implicit val invokeContractJsonEncoder: Encoder[InvokeContract] = (c: InvokeContract) => {
    Json.obj(
      "id"        -> Json.fromString(c.id.value),
      "invoker"   -> Json.fromString(c.invoker.value),
      "name"      -> Json.fromString(c.name.value),
      "version"   -> Json.fromString(c.version.value),
      "function"  -> Json.fromString(c.function.name),
      "parameter" -> c.parameter.asJson,
      "signature" -> Json.fromString(c.signature.base64),
      "status"    -> c.status.asJson
    )
  }

  implicit val invokeContractJsonDecoder: Decoder[InvokeContract] = (c: HCursor) => {
    for {
      id        <- c.get[String]("id")
      invoker   <- c.get[String]("invoker")
      name      <- c.get[String]("name")
      version   <- c.get[String]("version")
      function  <- c.get[String]("function")
      parameter <- c.get[Contract.Parameter]("parameter")
      signature <- c.get[String]("signature")
      status    <- c.get[Transaction.Status]("status")
    } yield
      InvokeContract(
        Transaction.ID(id),
        Account.ID(invoker),
        Contract.Name(name),
        Contract.Version(version),
        Contract.Function(function),
        parameter,
        Signature(BytesValue.decodeBase64(signature)),
        status
      )
  }

  implicit val transactionJsonEncoder: Encoder[Transaction] = {
    case t: Transfer =>
      Json.obj(
        "type" -> Json.fromString(TransactionType_Transfer),
        "impl" -> transferJsonEncoder(t)
      )

    case t: PublishContract =>
      Json.obj(
        "type" -> Json.fromString(TransactionType_PubContract),
        "impl" -> publishContractJsonEncoder(t)
      )

    case t: InvokeContract =>
      Json.obj(
        "type" -> Json.fromString(TransactionType_InvContract),
        "impl" -> invokeContractJsonEncoder(t)
      )

  }

  implicit val transactionJsonDecoder: Decoder[Transaction] = (c: HCursor) => {
    c.get[String]("type") match {
      case Left(t) => Left(t): Result[Transaction]
      case Right(TransactionType_Transfer) =>
        c.get[Json]("impl") match {
          case Left(t)     => Left(t): Result[Transaction]
          case Right(json) => json.as[Transfer]
        }

      case Right(TransactionType_PubContract) =>
        c.get[Json]("impl") match {
          case Left(t)     => Left(t): Result[Transaction]
          case Right(json) => json.as[PublishContract]
        }

      case Right(TransactionType_InvContract) =>
        c.get[Json]("impl") match {
          case Left(t)     => Left(t): Result[Transaction]
          case Right(json) => json.as[InvokeContract]
        }

      case Right(_) => ???
    }
  }
}

object TransactionJsonCodec extends TransactionJsonCodec
