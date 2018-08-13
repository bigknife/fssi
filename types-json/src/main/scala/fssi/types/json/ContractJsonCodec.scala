package fssi
package types
package json

import types._
import types.implicits._

import utils._
import io.circe._
import io.circe.syntax._
import implicits._

import scala.collection._

trait ContractJsonCodec {
  implicit val contractPrimaryParameterJsonEncoder: Encoder[Contract.Parameter.PrimaryParameter] =
    (a: Contract.Parameter.PrimaryParameter) =>
      a match {
        case x: Contract.Parameter.PString     => Json.fromString(x.value)
        case x: Contract.Parameter.PBigDecimal => x.value.asJson
        case x: Contract.Parameter.PBool       => Json.fromBoolean(x.value)
    }

  implicit val contractPrimaryParameterJsonDecoder: Decoder[Contract.Parameter.PrimaryParameter] =
    (a: HCursor) => {
      a.as[String].right.map(x => Contract.Parameter.PString(x)) match {
        case Left(_) =>
          a.as[java.math.BigDecimal].right.map(x => Contract.Parameter.PBigDecimal(x)) match {
            case Left(_) =>
              a.as[Boolean].right.map(x => Contract.Parameter.PBool(x))
            case y @ Right(_) => y
          }
        case x @ Right(_) => x
      }
    }

  implicit val contractPArrayJsonEncoder: Encoder[Contract.Parameter.PArray] =
    (a: Contract.Parameter.PArray) => a.asJson

  implicit val contractPArrayJsonDecoder: Decoder[Contract.Parameter.PArray] =
    (a: HCursor) => a.as[Contract.Parameter.PArray]


  implicit val contractParamJsonEncoder: Encoder[Contract.Parameter] = (a: Contract.Parameter) => {
    a match {
      case x: Contract.Parameter.PrimaryParameter => x.asJson
      case x: Contract.Parameter.PArray => x.asJson
    }
  }

  implicit val contractParamJsonDecoder: Decoder[Contract.Parameter] = (a: HCursor) => {
    a.as[Contract.Parameter.PrimaryParameter] match {
      case Left(t) =>
        a.as[Contract.Parameter.PArray]
      case x@Right(_) => x
    }
  }

  implicit val contractMethodJsonEncoder: Encoder[Contract.Method] = (a: Contract.Method) =>
    Json.obj(
      "className"  -> Json.fromString(a.className),
      "methodName" -> Json.fromString(a.methodName)
  )

  implicit val contractMethodJsonDecoder: Decoder[Contract.Method] = (h: HCursor) => {
    for {
      className  <- h.get[String]("className")
      methodName <- h.get[String]("methodName")
    } yield Contract.Method(className, methodName)
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
