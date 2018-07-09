package fssi.interpreter.codec

import fssi.ast.domain.types.Moment
import io.circe.{Encoder, Json}
import fssi.interpreter.codec._
import io.circe.syntax._
import io.circe.generic.semiauto._

trait MomentJsonCodec {
  implicit val momentCirceEncoder: Encoder[Moment] = new Encoder[Moment] {
    override def apply(a: Moment): Json = {
     a.asJson
    }
  }
}
