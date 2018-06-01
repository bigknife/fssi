package fssi.interpreter.codec

import fssi.ast.domain.types.Account
import io.circe.{Encoder, Json}

trait AccountJsonCodec {
  implicit val accountCirceEncoder = new Encoder[Account] {
    override def apply(a: Account): Json = Json.obj(
      "id" -> Json.fromString(a.id.value),
      "privateKey" -> Json.fromString(a.privateKeyData.hex),
      "publicKey" -> Json.fromString(a.publicKeyData.hex),
      "iv" -> Json.fromString(a.iv.hex),
      "balance" -> TokenJsonCodec.tokenJsonEncoder(a.balance)
    )
  }
}

object AccountJsonCodec extends AccountJsonCodec