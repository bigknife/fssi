package fssi.interpreter.codec

import fssi.ast.domain.types.Contract.Parameter._
import fssi.ast.domain.types.{BytesValue, Contract}
import io.circe._

trait ContractJsonCodec {
  implicit val contractJsonEncoder: Encoder[Contract.UserContract] = (c: Contract.UserContract) => {
    Json.obj(
      "name"     -> Json.fromString(c.name.value),
      "version"  -> Json.fromString(c.version.value),
      "code"     -> Json.fromString(c.code.base64),
      "codeHash" -> Json.fromString(c.codeHash.base64)
    )
  }

  implicit val contractJsonDecoder: Decoder[Contract.UserContract] = (c: HCursor) => {
    for {
      name     <- c.get[String]("name")
      version  <- c.get[String]("version")
      code     <- c.get[String]("code")
      codeHash <- c.get[String]("codeHash")
    } yield
      Contract.UserContract(
        Contract.Name(name),
        Contract.Version(version),
        Contract.Code(code),
        BytesValue.decodeBase64(codeHash)
      )
  }

  implicit val contractParameterEncoder: Encoder[Contract.Parameter] = {
    case x: Contract.Parameter.PString     => Json.fromString(x.value)
    case x: Contract.Parameter.PBigDecimal => Json.fromBigDecimal(x.value)
    case x: Contract.Parameter.PBool       => Json.fromBoolean(x.value)
    case xs: Contract.Parameter.PArray =>
      Json.fromValues(xs.array.map {
        case x0: Contract.Parameter.PString     => Json.fromString(x0.value)
        case x0: Contract.Parameter.PBigDecimal => Json.fromBigDecimal(x0.value)
        case x0: Contract.Parameter.PBool       => Json.fromBoolean(x0.value)
      })
  }

  implicit val contractParameterDecoder: Decoder[Contract.Parameter] =
    (c: HCursor) => {
      c.as[String]
        .map(PString)
        .left
        .flatMap(_ => c.as[BigDecimal].map(x => PBigDecimal(x.bigDecimal)))
        .left
        .flatMap(_ => c.as[Boolean].map(PBool))
        .left
        .flatMap(_ =>
          c.values match {
            case None => Right(PArray())
            case Some(arr) =>
              val xs: Iterable[Either[DecodingFailure, PrimaryParameter]] = arr.map { json =>
                json
                  .as[String]
                  .toOption
                  .map(PString)
                  .orElse(json.as[BigDecimal].toOption.map(x => PBigDecimal(x.bigDecimal)))
                  .orElse(json.as[Boolean].toOption.map(PBool)) match {
                  case None    => Left(DecodingFailure("Unsupported Json Type", List()))
                  case Some(j) => Right(j)
                }
              }
              xs.find(_.isLeft) match {
                case Some(x) => x
                case None    => Right(PArray(xs.map(_.right.get).toArray))
              }
        })
    }
}
object ContractJsonCodec extends ContractJsonCodec
