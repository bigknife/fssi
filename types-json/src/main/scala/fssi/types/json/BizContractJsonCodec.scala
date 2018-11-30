package fssi
package types
package json
import fssi.base.BytesValue
import fssi.types.biz._
import fssi.types.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import json.implicits._
import fssi.types.biz.Contract.UserContract._
import fssi.types.biz.Contract.UserContract.Parameter._

trait BizContractJsonCodec {
  implicit val userContractCodeEncoder: Encoder[Contract.UserContract.Code] =
    Encoder[String].contramap(_.asBytesValue.bcBase58)

  implicit val userContractCodeDecoder: Decoder[Contract.UserContract.Code] =
    Decoder[String].map(x => Contract.UserContract.Code(BytesValue.decodeBcBase58(x).get.bytes))

  implicit val bizVersionEncoder: Encoder[Contract.Version] =
    Encoder[String].contramap(_.toString)

  implicit val bizVersionDecoder: Decoder[Contract.Version] =
    Decoder[String].map { x =>
      Contract.Version(x) match {
        case Some(v) => v
        case None    => throw DecodingFailure("contract version should be maj.min.patch", List())
      }
    }

  implicit val bizPStringEncoder: Encoder[PString] =
    Encoder[String].contramap(_.value)

  implicit val bizPStringDecoder: Decoder[PString] =
    Decoder[String].map(PString)

  implicit val bizPBigDecimalEncoder: Encoder[PBigDecimal] =
    Encoder[java.math.BigDecimal].contramap(_.value)

  implicit val bizPBigDecimalDecoder: Decoder[PBigDecimal] =
    Decoder[java.math.BigDecimal].map(PBigDecimal(_))

  implicit val bizPBoolEncoder: Encoder[PBool] =
    Encoder[Boolean].contramap(_.value)

  implicit val bizPBoolDecoder: Decoder[PBool] =
    Decoder[Boolean].map(PBool)

  implicit val bizPArrayEncoder: Encoder[PArray] =
    (a: PArray) => {
      Json.arr(a.array.map(_.asJson): _*)
    }

  implicit val bizPrimaryParameterJsonEncoder: Encoder[PrimaryParameter] = {
    case x: PString     => Json.fromString(x.value)
    case x: PBigDecimal => Json.fromBigDecimal(x.value)
    case x: PBool       => Json.fromBoolean(x.value)
  }

  implicit val bizContractParamJsonEncoder: Encoder[Parameter] = {
    case x: PrimaryParameter => x.asJson
    case x: PArray           => x.asJson
  }

  implicit val bizUserContractPrimaryParameterDecoder: Decoder[PrimaryParameter] =
    (a: HCursor) => {
      a.as[String].right.map(x => PString(x)) match {
        case Left(_) =>
          a.as[java.math.BigDecimal].right.map(x => PBigDecimal(x)) match {
            case Left(_) =>
              a.as[Boolean].right.map(x => PBool(x))
            case y @ Right(_) => y
          }
        case x @ Right(_) => x
      }
    }

  implicit val bizUserContractPArrayJsonDecoder: Decoder[PArray] = (a: HCursor) => {
    a.values match {
      case Some(jsons) =>
        def _loop(jso: Vector[Json], acc: Decoder.Result[PArray]): Decoder.Result[PArray] = {
          if (jso.isEmpty || acc.isLeft) acc
          else {
            val head = jso.head
            val c = for {
              hp     <- head.as[PrimaryParameter]
              parray <- acc
            } yield PArray(parray.array :+ hp)
            _loop(jso.tail, c)
          }
        }
        _loop(jsons.toVector, Right(PArray()))

      case None => Left(DecodingFailure("should be an array", List()))
    }
  }

  implicit val bizUserContractParamJsonDecoder: Decoder[Parameter] = (a: HCursor) => {
    a.as[PrimaryParameter] match {
      case Left(_)      => a.as[PArray]
      case x @ Right(_) => x
    }
  }

  implicit val bizUserContractDescJsonEncoder: Encoder[Description] =
    Encoder[String].contramap(_.value.asBytesValue.bcBase58)

  implicit val bizUserContractDescJsonDecoder: Decoder[Description] =
    Decoder[String].map(x => Description(BytesValue.decodeBcBase58(x).get.bytes))
}
