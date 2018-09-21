package fssi
package types
package json

import fssi.types.base._
import fssi.types.biz._
import fssi.types.implicits._

import io.circe._
import io.circe.syntax._

trait BizTokenJsonCodec {
  implicit val bizTokenEncoder: Encoder[Token] = Encoder[String].contramap(_.toString)
}
