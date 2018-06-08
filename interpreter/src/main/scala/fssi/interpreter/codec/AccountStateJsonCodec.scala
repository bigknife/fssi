package fssi.interpreter.codec

import fssi.contract.AccountState
import io.circe._
import io.circe.syntax._

trait AccountStateJsonCodec {
  implicit val accountStateJsonEncoder: Encoder[AccountState] = (a: AccountState) => {
    Json.obj(
      "accountId" -> Json.fromString(a.accountId),
      "amount"    -> Json.fromBigDecimal(a.amount),
      "assets"    -> a.assets.asJson
    )
  }

  implicit val accountStateJsonDecoder: Decoder[AccountState] = (c: HCursor) => {
    for {
      accountId <- c.get[String]("accountId")
      amount    <- c.get[BigDecimal]("amount")
      assets    <- c.get[Map[String, Array[Byte]]]("assets")
    } yield AccountState(accountId, amount, assets)
  }
}

object AccountStateJsonCodec extends AccountStateJsonCodec
