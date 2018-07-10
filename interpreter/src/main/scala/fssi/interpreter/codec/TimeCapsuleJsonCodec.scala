package fssi.interpreter.codec

import fssi.ast.domain.types.TimeCapsule
import io.circe.{Encoder, Json}
import fssi.interpreter.jsonCodec._
import io.circe.syntax._

trait TimeCapsuleJsonCodec {
  implicit val timeCapsuleJsonEncoder: Encoder[TimeCapsule] = new Encoder[TimeCapsule] {
    override def apply(a: TimeCapsule): Json = Json.obj(
      "height" -> Json.fromString(a.height.toString),
      "moments" -> a.moments.asJson,
      "hash" -> Json.fromString(a.hash.hex),
      "previousHash" -> Json.fromString(a.previousHash.hex)
    )
  }
}
