package fssi
package types
package json

import types._,implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait BlockJsonCodec {
  implicit val blockJsonEncoder: Encoder[Block] = (a: Block) => Json.obj(
    "hash" -> a.hash.asJson,
    "previousHash" -> a.previousHash.asJson,
    "height" -> a.height.asJson,
    "chainID" -> a.chainID.asJson,
    "transactions" -> Json.fromValues(a.transactions.map(_.asJson))
  )
}
