package fssi.interpreter.codec

import fssi.ast.domain.types.Token
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

trait TokenJsonCodec {
  implicit val tokenJsonEncoder: Encoder[Token] = (a: Token) =>
    Json.obj(
      "amount" -> Json.fromLong(a.amount),
      "unit"   -> Json.fromString(a.unit.toString)
  )

  implicit val tokenJsonDecoder: Decoder[Token] = new Decoder[Token] {
    override def apply(c: HCursor): Result[Token] =
      for {
        ammount <- c.get[Long]("amount")
        u       <- c.get[String]("unit")
      } yield Token(ammount, Token.Unit(u))
  }
}

object TokenJsonCodec extends TokenJsonCodec
