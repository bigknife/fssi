package fssi
package types
package json

import types._
import io.circe._
import io.circe.syntax._
import implicits._

trait AccountCodec {
  implicit val accountIDEncoder: Encoder[Account.ID] = (a: Account.ID) => a.value.asJson
  implicit val accountIDDecoder: Decoder[Account.ID] = (h: HCursor) => {
    for {
      value <- h.as[HexString]
    } yield Account.ID(value)
  }

  implicit val accountEncoder: Encoder[Account] = (a: Account) => {
    Json.obj(
      "publicKey" -> a.publicKey.asJson,
      "encryptedPrivateKey" -> a.encryptedPrivateKey.asJson,
      "iv" -> a.iv.asJson
    )
  }

  implicit val accountDecoder: Decoder[Account] = (c: HCursor) => {
    for {
      publicKey <- c.get[HexString]("publicKey")
      encryptedPrivateKey <- c.get[HexString]("encryptedPrivateKey")
      iv <- c.get[HexString]("iv")
    } yield Account(publicKey, encryptedPrivateKey, iv)
  }
}
