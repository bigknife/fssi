package fssi.interpreter.codec

import fssi.ast.domain.types.{Account, BytesValue, Token}
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

trait AccountJsonCodec extends TokenJsonCodec{
  implicit val accountCirceEncoder: Encoder[Account] = (a: Account) =>
    Json.obj(
      "id"         -> Json.fromString(a.id.value),
      "privateKey" -> Json.fromString(a.privateKeyData.hex),
      "publicKey"  -> Json.fromString(a.publicKeyData.hex),
      "iv"         -> Json.fromString(a.iv.hex),
      "balance"    -> TokenJsonCodec.tokenJsonEncoder(a.balance)
  )

  implicit val accountCirceDecoder: Decoder[Account] = (c: HCursor) => {
    for {
      id <- c.get[String]("id")
      privateKey <- c.get[String]("privateKey")
      publicKey <- c.get[String]("publicKey")
      iv <- c.get[String]("iv")
      balance <- c.get[Token]("balance")
    } yield
      Account(
        Account.ID(id),
        BytesValue.decodeHex(privateKey),
        BytesValue.decodeHex(publicKey),
        BytesValue.decodeHex(iv),
        balance
      )
  }
}

object AccountJsonCodec extends AccountJsonCodec
