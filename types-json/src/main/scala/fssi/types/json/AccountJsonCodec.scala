package fssi
package types
package json

import types.biz._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import fssi.types.implicits._

trait AccountJsonCodec {
  implicit val bizAccountPrivKeyEncoder: Encoder[Account.PrivKey] = (a: Account.PrivKey) =>
    a.asBytesValue.bcBase58.asJson
  implicit val bizAccountPubKeyEncoder: Encoder[Account.PubKey] = (a: Account.PubKey) =>
    a.asBytesValue.bcBase58.asJson
  implicit val bizAccountIDEncoder: Encoder[Account.ID] = (a: Account.ID) =>
    a.asBytesValue.bcBase58.asJson
  implicit val bizAccountIVEncoder: Encoder[Account.IV] = (a: Account.IV) =>
    a.asBytesValue.bcBase58.asJson
  implicit val bizAccountSecretKeyEncoder: Encoder[Account.SecretKey] = (a: Account.SecretKey) =>
    a.asBytesValue.bcBase58.asJson
}
