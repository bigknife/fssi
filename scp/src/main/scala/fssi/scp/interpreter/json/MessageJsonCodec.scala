package fssi
package scp
package interpreter
package json
import types._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import implicits._

trait MessageJsonCodec {

  implicit def messageEncoder(implicit valueEncoder: Encoder[Value]): Encoder[Message] = {
    case nominate: Message.Nomination => nominate.asJson
    case ballotMessage: Message.BallotMessage =>
      ballotMessage match {
        case prepare: Message.Prepare => prepare.asJson
        case confirm: Message.Confirm => confirm.asJson
        case ext: Message.Externalize => ext.asJson
      }
  }

  implicit def messageDecoder(implicit valueDecoder: Decoder[Value]): Decoder[Message] =
    (hCursor: HCursor) =>
      hCursor.as[Message.Nomination] match {
        case Right(nominate) => Right(nominate)
        case Left(_) =>
          def toPrepare: Decoder.Result[Message.Prepare] = hCursor.as[Message.Prepare]
          def toConfirm: Decoder.Result[Message.Confirm] = hCursor.as[Message.Confirm]
          def toExt: Decoder.Result[Message.Externalize] = hCursor.as[Message.Externalize]
          if (toPrepare.isRight) toPrepare else if (toConfirm.isRight) toConfirm else toExt
    }
}
