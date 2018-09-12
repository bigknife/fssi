package fssi
package types
package json

import fssi.types.Contract.Parameter.{PArray, PBigDecimal, PBool, PString}
import types._
import types.implicits._
import utils._
import io.circe._
import io.circe.syntax._
import implicits._

import scala.collection._

trait ContractJsonCodec {
  implicit val contractPrimaryParameterJsonEncoder: Encoder[Contract.Parameter.PrimaryParameter] = {
    case x: PString     => Json.fromString(x.value)
    case x: PBigDecimal => x.value.asJson
    case x: PBool       => Json.fromBoolean(x.value)
  }

  implicit val contractPrimaryParameterJsonDecoder: Decoder[Contract.Parameter.PrimaryParameter] =
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

  implicit val contractPArrayJsonEncoder: Encoder[PArray] =
    (a: PArray) => {
      Json.arr(a.array.map(_.asJson): _*)
    }

  implicit val contractPArrayJsonDecoder: Decoder[PArray] = (a: HCursor) => {
    a.values match {
      case Some(jsons) =>
        def _loop(jso: Vector[Json], acc: Decoder.Result[PArray]): Decoder.Result[PArray] = {
          if (jso.isEmpty || acc.isLeft) acc
          else {
            val head = jso.head
            val c = for {
              hp <- head.as[Contract.Parameter.PrimaryParameter]
              parray <- acc
            } yield PArray(parray.array :+ hp)
            _loop(jso.tail, c)
          }
        }
        _loop(jsons.toVector, Right(PArray()))


      case None => Left(DecodingFailure("should be an array", List()))
    }
  }

  implicit val contractParamJsonEncoder: Encoder[Contract.Parameter] = {
    case x: Contract.Parameter.PrimaryParameter => x.asJson
    case x: PArray                              => x.asJson
    case Contract.Parameter.PEmpty              => Json.Null
  }

  implicit val contractParamJsonDecoder: Decoder[Contract.Parameter] = (a: HCursor) => {
    if (a.value == Json.Null) Right(Contract.Parameter.PEmpty)
    else {
      a.as[Contract.Parameter.PrimaryParameter] match {
        case Left(_) => a.as[PArray]
        case x@Right(_) => x
      }
    }
  }

  implicit val contractMethodJsonEncoder: Encoder[Contract.Method] = (a: Contract.Method) =>
    Json.obj("alias" -> Json.fromString(a.alias))

  implicit val contractMethodJsonDecoder: Decoder[Contract.Method] = (h: HCursor) => {
    for {
      alias <- h.get[String]("alias")
    } yield Contract.Method(alias)
  }

  implicit val contractMetaJsonEncoder: Encoder[Contract.Meta] = (a: Contract.Meta) =>
    Json.obj(
      "methods" -> a.methods.toVector.asJson
  )

  implicit val contractMetaJsonDecoder: Decoder[Contract.Meta] = (h: HCursor) => {
    for {
      methods <- h.get[Vector[Contract.Method]]("methods")
    } yield Contract.Meta(immutable.TreeSet(methods: _*))
  }

  implicit val userContractJsonEncoder: Encoder[Contract.UserContract] =
    (a: Contract.UserContract) =>
      Json.obj(
        "owner"     -> a.owner.asJson,
        "name"      -> a.name.asJson,
        "version"   -> a.version.asJson,
        "code"      -> a.code.asJson,
        "meta"      -> a.meta.asJson,
        "signature" -> a.signature.asJson
    )

  implicit val userContractJsonDecoder: Decoder[Contract.UserContract] = (h: HCursor) => {
    for {
      owner     <- h.get[Account.ID]("owner")
      name      <- h.get[UniqueName]("name")
      version   <- h.get[Version]("version")
      code      <- h.get[Base64String]("code")
      meta      <- h.get[Contract.Meta]("meta")
      signature <- h.get[Signature]("signature")
    } yield Contract.UserContract(owner, name, version, code, meta, signature)
  }

}
