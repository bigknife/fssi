package fssi
package types
package json

import types._,implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait HashJsonCodec {
  implicit val hashJsonEncoder: Encoder[Hash] = (a: Hash) => Json.fromString(a.toString)
}
