package fssi
package scp
package interpreter
package json
import types._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import implicits._

trait MessageJsonCodec {

  implicit def messageEncoder(implicit valueEncoder: Encoder[Value]): Encoder[Message] = {
    case nominate: Message.Nomination =>
      Map("type" -> "nominate", "body" -> nominate.asJson.noSpaces).asJson
      //nominate.asJson
    case ballotMessage: Message.BallotMessage =>
      ballotMessage match {
        case prepare: Message.Prepare =>
          Map("type" -> "prepare", "body" -> prepare.asJson.noSpaces).asJson
          //prepare.asJson
        case confirm: Message.Confirm =>
          Map("type" -> "confirm", "body" -> confirm.asJson.noSpaces).asJson
          //confirm.asJson
        case ext: Message.Externalize =>
          Map("type" -> "externalize", "body" -> ext.asJson.noSpaces).asJson
          //ext.asJson
      }
  }

  implicit def messageDecoder(implicit valueDecoder: Decoder[Value]): Decoder[Message] = {hCursor =>
    val r0 = for {
      t <- hCursor.get[String]("type")
      js <- hCursor.get[String]("body")
    } yield (t, js)

    r0 match {
      case Right(("nominate", js)) =>
        val x: Either[Error, Message.Nomination] = for {
          jso <- parse(js)
          m <- jso.as[Message.Nomination]
        } yield m
        x match {
          case Left(t) => Left(DecodingFailure.fromThrowable(t, List(CursorOp.DownField("body"))))
          case Right(x) => Right(x)
        }
      case Right(("prepare", js)) =>
        val x: Either[Error, Message.Prepare] = for {
          jso <- parse(js)
          m <- jso.as[Message.Prepare]
        } yield m
        x match {
          case Left(t) => Left(DecodingFailure.fromThrowable(t, List(CursorOp.DownField("body"))))
          case Right(x) => Right(x)
        }
      case Right(("confirm", js)) =>
        val x: Either[Error, Message.Confirm] = for {
          jso <- parse(js)
          m <- jso.as[Message.Confirm]
        } yield m
        x match {
          case Left(t) => Left(DecodingFailure.fromThrowable(t, List(CursorOp.DownField("body"))))
          case Right(x) => Right(x)
        }
      case Right(("externalize", js)) =>
        val x: Either[Error, Message.Externalize] = for {
          jso <- parse(js)
          m <- jso.as[Message.Externalize]
        } yield m
        x match {
          case Left(t) => Left(DecodingFailure.fromThrowable(t, List(CursorOp.DownField("body"))))
          case Right(x) => Right(x)
        }

      case Left(t) => Left(t)
    }
  }
}
