package fssi.interpreter.codec

import fssi.ast.domain.types.Token
import io.circe.{Encoder, Json}

trait TokenJsonCodec {
  implicit val tokenJsonEncoder: Encoder[Token] = new Encoder[Token] {
    override def apply(a: Token): Json = Json.obj(
      "amount" -> Json.fromLong(a.amount),
      "unit" -> Json.fromString(a.unit.toString)
    )
  }
}

object TokenJsonCodec extends TokenJsonCodec
