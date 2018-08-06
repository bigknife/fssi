package fssi
package types
package json

import types._
import io.circe._

trait AccountCodec {
  implicit val accountEncoder: Encoder[Account] = (a: Account) => {
    Json.obj(
      "publicKey" -> Json.fromString(a.publicKey.toString),
      "encryptedPrivateKey" -> Json.fromString(a.encryptedPrivateKey.toString),
      "iv" -> Json.fromString(a.iv.toString)
    )
  }

  implicit val accountDecoder: Decoder[Account] = (c: HCursor) => {
    for {
      publicKey <- c.get[String]("publicKey")
      encryptedPrivateKey <- c.get[String]("encryptedPrivateKey")
      iv <- c.get[String]("iv")
    } yield Account(HexString.decode(publicKey), HexString.decode(encryptedPrivateKey), HexString.decode(iv))
  }
}
