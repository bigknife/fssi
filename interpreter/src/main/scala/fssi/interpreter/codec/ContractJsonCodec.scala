package fssi.interpreter.codec

import fssi.ast.domain.types.Contract.Parameter._
import fssi.ast.domain.types.{BytesValue, Contract, Signature}
import io.circe.Decoder.Result
import io.circe._

trait ContractJsonCodec {
  implicit val contractJsonEncoder: Encoder[Contract] = (c: Contract) => {
    Json.obj(
      "name"     -> Json.fromString(c.name.value),
      "version"  -> Json.fromString(c.version.value),
      "code"     -> Json.fromString(c.code.base64),
      "codeSign" -> Json.fromString(c.codeSign.base64)
    )
  }

  implicit val contractJsonDecoder: Decoder[Contract] = (c: HCursor) => {
    for {
      name     <- c.get[String]("name")
      version  <- c.get[String]("version")
      code     <- c.get[String]("code")
      codeSign <- c.get[String]("codeSign")
    } yield
      Contract(
        Contract.Name(name),
        Contract.Version(version),
        Contract.Code(code),
        Signature(BytesValue.decodeBase64(codeSign))
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

  implicit val contractParameterDecoder: Decoder[Contract.Parameter] = new Decoder[Contract.Parameter] {
    override def apply(c: HCursor): Result[Contract.Parameter] = {
      c.as[String]
        .map(PString)
        .left
        .flatMap(_ => c.as[BigDecimal].map(x => PBigDecimal(x.bigDecimal)))
        .left
        .flatMap(_ => c.as[Boolean].map(PBool))
        .left
        .flatMap(_ =>
          c.values match {
            //case None => ???
            case Some(arr) =>
              val xs: Iterable[Either[DecodingFailure, PrimaryParameter]] = arr.map { json =>
                json.as[String].toOption.map(PString)
                    .orElse(json.as[BigDecimal].toOption.map(x => PBigDecimal(x.bigDecimal)))
                    .orElse(json.as[Boolean].toOption.map(PBool)) match {
                  case None => Left(DecodingFailure("Unsupported Json Type", List()))
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
}
object ContractJsonCodec extends ContractJsonCodec
