package fssi.interpreter.codec

import fssi.ast.domain.types.BytesValue
import fssi.contract.{AccountState, States}
import io.circe._
import io.circe.syntax._

trait AccountStateJsonCodec {
  //todo: re encode assets json
  implicit val accountStateJsonEncoder: Encoder[AccountState] = (a: AccountState) => {
    Json.obj(
      "accountId" -> Json.fromString(a.accountId),
      "amount"    -> Json.fromBigDecimal(a.amount),
      "assets"    -> a.assets.mapValues(BytesValue.apply(_).base64).asJson
    )
  }

  implicit val accountStateJsonDecoder: Decoder[AccountState] = (c: HCursor) => {
    for {
      accountId <- c.get[String]("accountId")
      amount    <- c.get[BigDecimal]("amount")
      assets    <- c.get[Map[String, String]]("assets")
    } yield AccountState(accountId, amount, assets.mapValues(BytesValue.decodeBase64(_).bytes))
  }

  implicit val accountStatesJsonEncoder: Encoder[States] = (s: States) => {
    Json.obj(
      "states" -> s.states.asJson
    )
  }

  implicit val accountStatesJsonDecoder: Decoder[States] = (c: HCursor) => {
    for {
      states <- c.get[Map[String, AccountState]]("states")
    } yield States(states)
  }
}

object AccountStateJsonCodec extends AccountStateJsonCodec
