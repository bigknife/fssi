package fssi.interpreter.codec

import fssi.contract.AccountState
import io.circe._

trait AccountStateJsonCodec {
  implicit val accountStateJsonEncoder: Encoder[AccountState] = ???
  implicit val accountStateJsonDecoder: Decoder[AccountState] = ???
}

object AccountStateJsonCodec extends AccountStateJsonCodec
