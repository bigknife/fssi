package fssi.interpreter.codec

import fssi.ast.domain.types._
import io.circe.{Decoder, Encoder, HCursor, Json}
import fssi.interpreter.jsonCodec._
import io.circe.Decoder.Result
import io.circe.syntax._

trait TimeCapsuleJsonCodec {
  implicit val timeCapsuleJsonEncoder: Encoder[TimeCapsule] = new Encoder[TimeCapsule] {
    override def apply(a: TimeCapsule): Json = Json.obj(
      "height"       -> Json.fromString(a.height.toString),
      "moments"      -> a.moments.asJson,
      "hash"         -> Json.fromString(a.hash.hex),
      "previousHash" -> Json.fromString(a.previousHash.hex)
    )
  }

  implicit val timeCapsuleJsonDecoder: Decoder[TimeCapsule] = new Decoder[TimeCapsule] {
    override def apply(c: HCursor): Result[TimeCapsule] = {
      for {
        height       <- c.get[String]("height")
        moments      <- c.get[Vector[Moment]]("moments")
        hash         <- c.get[String]("hash")
        previousHash <- c.get[String]("previousHash")
      } yield
        TimeCapsule(BigInt(height),
                    moments,
                    Hash.fromHexString(hash),
                    Hash.fromHexString(previousHash))
    }
  }
}
