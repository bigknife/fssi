package fssi.interpreter.codec

import fssi.ast.domain.types.Account
import io.circe.{Encoder, Json}

trait AccountJsonCodec {
  implicit val accountCirceEncoder = new Encoder[Account] {
    override def apply(a: Account): Json = ???
  }
}
