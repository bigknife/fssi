package fssi.interpreter.codec

import fssi.ast.domain.types.{BytesValue, Contract, Signature}
import io.circe.{Decoder, Encoder, HCursor, Json}

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
}
object ContractJsonCodec extends ContractJsonCodec
