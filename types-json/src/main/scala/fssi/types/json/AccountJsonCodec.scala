package fssi
package types
package json

import types.biz._
import types.base._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import fssi.types.implicits._

trait AccountJsonCodec {
  implicit val bizAccountPrivKeyEncoder: Encoder[Account.PrivKey] = (a: Account.PrivKey) =>
    a.asBytesValue.bcBase58.asJson

  implicit val bizAccountPrivKeyDecoder: Decoder[Account.PrivKey] =
    Decoder[String].map(s => Account.PrivKey(BytesValue.decodeBcBase58(s).get.value))

  implicit val bizAccountPubKeyEncoder: Encoder[Account.PubKey] = (a: Account.PubKey) =>
    a.asBytesValue.bcBase58.asJson

  implicit val bizAccountPubKeyDecoder: Decoder[Account.PubKey] =
    Decoder[String].map(s => Account.PubKey(BytesValue.decodeBcBase58(s).get.value))

  implicit val bizAccountIDEncoder: Encoder[Account.ID] = (a: Account.ID) =>
    a.asBytesValue.bcBase58.asJson

  implicit val bizAccountIDDecoder: Decoder[Account.ID] =
    Decoder[String].map(s => Account.ID(BytesValue.decodeBcBase58(s).get.value))

  implicit val bizAccountIVEncoder: Encoder[Account.IV] = (a: Account.IV) =>
    a.asBytesValue.bcBase58.asJson

  implicit val bizAccountIVDecoder: Decoder[Account.IV] =
    Decoder[String].map(s => Account.IV(BytesValue.decodeBcBase58(s).get.value))

  implicit val bizAccountSecretKeyEncoder: Encoder[Account.SecretKey] = (a: Account.SecretKey) =>
    a.asBytesValue.bcBase58.asJson

  implicit val bizAccountSecretKeyDecoder: Decoder[Account.SecretKey] =
    Decoder[String].map(s => Account.SecretKey(BytesValue.decodeBcBase58(s).get.value))
}
