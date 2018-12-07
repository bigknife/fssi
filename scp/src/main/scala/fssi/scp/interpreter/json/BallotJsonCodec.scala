package fssi
package scp
package interpreter
package json

import types._
import io.circe._
import io.circe.syntax._
import implicits._

trait BallotJsonCodec {

  implicit def ballotEncoder(implicit valueEncoder: Encoder[Value]): Encoder[Ballot] =
    (ballot: Ballot) =>
      if (ballot.isBottom) Json.obj("counter" -> Json.fromInt(ballot.counter))
      else Json.obj("counter"                 -> Json.fromInt(ballot.counter), "value" -> ballot.value.asJson)

  implicit def ballotDecoder(implicit valueDecoder: Decoder[Value]): Decoder[Ballot] =
    (hCursor: HCursor) =>
      hCursor.get[Value]("value") match {
        case Left(_) => Right(Ballot.bottom)
        case Right(v) =>
          hCursor.get[Int]("counter").map(c => Ballot(c, v))
    }
}
