package fssi.interpreter.codec

import fssi.ast.domain.types.{BytesValue, Moment, Transaction}
import fssi.contract.States
import io.circe.{Decoder, Encoder, HCursor, Json}
import fssi.interpreter.jsonCodec._
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.generic.semiauto._

trait MomentJsonCodec {
  implicit val momentCirceEncoder: Encoder[Moment] = new Encoder[Moment] {
    override def apply(a: Moment): Json = Json.obj(
      "oldStates"     -> a.oldStates.asJson,
      "transaction"   -> a.transaction.asJson,
      "newStates"     -> a.newStates.asJson,
      "oldStatesHash" -> a.oldStatesHash.hex.asJson,
      "newStatesHash" -> a.newStatesHash.hex.asJson,
      "timestamp"     -> Json.fromLong(a.timestamp)
    )
  }

  implicit val momentJsonDecoder: Decoder[Moment] = new Decoder[Moment] {
    override def apply(c: HCursor): Result[Moment] =
      for {
        oldStates     <- c.get[States]("oldStates")
        transaction   <- c.get[Transaction]("transaction")
        newStates     <- c.get[States]("newStates")
        oldStatesHash <- c.get[String]("oldStatesHash")
        newStatesHash <- c.get[String]("newStatesHash")
        timestamp     <- c.get[Long]("timestamp")
      } yield
        Moment(oldStates,
               transaction,
               newStates,
               BytesValue.decodeHex(oldStatesHash),
               BytesValue.decodeHex(newStatesHash),
               timestamp)
  }
}
