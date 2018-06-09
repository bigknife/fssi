package fssi.interpreter.codec

import fssi.ast.domain.ContractCodeItem
import io.circe.{Decoder, Encoder, HCursor, Json}

trait ContractCodeItemJsonCodec {
  implicit val contractCodeItemJsonEncoder: Encoder[ContractCodeItem] = (c: ContractCodeItem) => {
    Json.obj(
      "name"    -> Json.fromString(c.name),
      "version" -> Json.fromString(c.version),
      "code"    -> Json.fromString(c.code),
      "hash"    -> Json.fromString(c.hash)
    )
  }

  implicit val contractCodeItemJsonDecoder: Decoder[ContractCodeItem] = (c: HCursor) => {
    for {
      name    <- c.get[String]("name")
      version <- c.get[String]("version")
      code    <- c.get[String]("code")
      hash    <- c.get[String]("hash")
    } yield ContractCodeItem(name, version, code, hash)
  }
}
