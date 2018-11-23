package fssi
package scp
package interpreter
package json

import types._
import QuorumSet.{QuorumRef, QuorumSlices, Slices}
import QuorumSet.Slices.{Flat, Nest}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import implicits._
import fssi.base._
import fssi.base.implicits._
import fssi.scp.types.implicits._

trait QuorumSetJsonCodec {

  implicit val slicesEncoder: Encoder[Slices] = {
    case flat: Flat => flat.asJson
    case nest: Nest => nest.asJson
  }

  implicit val slicesDecoder: Decoder[Slices] = (hCursor: HCursor) =>
    hCursor.as[Nest] match {
      case Right(nest) => Right(nest)
      case Left(_) =>
        hCursor.as[Flat] match {
          case Right(flat) => Right(flat)
          case Left(e)     => Left(DecodingFailure(e.getMessage(), List()))
        }
  }

  implicit val quorumRefEncoder: Encoder[QuorumRef] =
    Encoder[String].contramap(x => x.asBytesValue.bcBase58)

  implicit val quorumRefDecoder: Decoder[QuorumRef] =
    Decoder[String].map(x => QuorumRef(BytesValue.unsafeDecodeBcBase58(x).bytes))

  implicit val quorumSetEncoder: Encoder[QuorumSet] = {
    case slices: QuorumSlices => slices.asJson
    case ref: QuorumRef       => ref.asJson
  }

  implicit val quorumSetDecoder: Decoder[QuorumSet] = (hCursor: HCursor) =>
    hCursor.as[QuorumSlices] match {
      case Right(slices) => Right(slices)
      case Left(_) =>
        hCursor.as[QuorumRef] match {
          case Right(ref) => Right(ref)
          case Left(e)    => Left(DecodingFailure(e.getMessage(), List()))
        }
  }
}
