package fssi
package types
package json

import types._, implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait TokenJsonCodec {
  implicit val tokenJsonEncoder: Encoder[Token] = (a: Token) => Json.obj(
    "amount" -> a.amount.asJson,
    "tokenUnit" -> Json.fromString(a.tokenUnit.toString)
  )

  implicit val tokenJsonDecoder: Decoder[Token] = (h: HCursor) => {
    for {
      amount <- h.get[BigInt]("amount")
      tokenUnit <- h.get[String]("tokenUnit")
    } yield Token(amount, Token.Unit(tokenUnit))
  }
}
