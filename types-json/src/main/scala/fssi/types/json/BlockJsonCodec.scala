package fssi
package types
package json

import scala.collection._

import types._
import types.implicits._
import io.circe._
import io.circe.syntax._
import implicits._

trait BlockJsonCodec {
  implicit val blockJsonEncoder: Encoder[Block] = (a: Block) =>
    Json.obj(
      "hash"                      -> a.hash.asJson,
      "previousHash"              -> a.previousHash.asJson,
      "previousTokenState"        -> a.previousTokenState.asJson,
      "previousContractState"     -> a.previousContractState.asJson,
      "previousContractDataState" -> a.previousContractDataState.asJson,
      "height"                    -> a.height.asJson,
      "chainID"                   -> a.chainID.asJson,
      "transactions"              -> Json.fromValues(a.transactions.map(_.asJson))
  )

  implicit val blockJsonDecoder: Decoder[Block] = (h: HCursor) => {
    for {
      hash                      <- h.get[Hash]("hash")
      previousHash              <- h.get[Hash]("previousHash")
      previousTokenState        <- h.get[HexString]("previousTokenState")
      previousContractState     <- h.get[HexString]("previousContractState")
      previousContractDataState <- h.get[HexString]("previousContractDataState")
      height                    <- h.get[BigInt]("height")
      chainID                   <- h.get[String]("chainID")
      transactions              <- h.get[Vector[Transaction]]("transactions")
    } yield
      Block(hash,
            previousHash,
            previousTokenState,
            previousContractState,
            previousContractDataState,
            height,
            immutable.TreeSet(transactions: _*),
            chainID)
  }
}
