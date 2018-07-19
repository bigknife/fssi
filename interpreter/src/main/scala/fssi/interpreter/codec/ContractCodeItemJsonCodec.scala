package fssi.interpreter.codec

import fssi.ast.domain.ContractCodeItem
import io.circe.{Decoder, Encoder, HCursor, Json}

trait ContractCodeItemJsonCodec {
  implicit val contractCodeItemJsonEncoder: Encoder[ContractCodeItem] = (c: ContractCodeItem) => {
    Json.obj(
      "owner"   -> Json.fromString(c.owner),
      "name"    -> Json.fromString(c.name),
      "version" -> Json.fromString(c.version),
      "code"    -> Json.fromString(c.code),
      "sig"     -> Json.fromString(c.sig)
    )
  }

  implicit val contractCodeItemJsonDecoder: Decoder[ContractCodeItem] = (c: HCursor) => {
    for {
      owner   <- c.get[String]("owner")
      name    <- c.get[String]("name")
      version <- c.get[String]("version")
      code    <- c.get[String]("code")
      sig     <- c.get[String]("sig")
    } yield ContractCodeItem(owner, name, version, code, sig)
  }
}
