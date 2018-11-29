package fssi
package types
package json

import fssi.types.base.{Signature, UniqueName}
import fssi.types.biz.Contract.UserContract.Parameter.Description
import fssi.types.biz.Contract.Version
import fssi.types.biz.{Account, Contract}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import org.scalatest.FunSuite
import json.implicits._
import types.implicits._

import scala.collection.immutable.TreeSet

class ContractJsonCodecSpec extends FunSuite {

  test("test contract json coder") {
    val owner   = Account.ID("owner".getBytes())
    val name    = UniqueName("name")
    val version = Version(0, 0, 1)
    val code    = Contract.UserContract.Code("code".getBytes())
    val method =
      Contract.UserContract.Method("register", "fssi.contract.Banana.register(Contract,String)")
    val methods   = TreeSet(method)
    val desc      = Description("contract description".getBytes("utf-8"))
    val signature = Signature("signature".getBytes())
    val userContract = Contract.UserContract(owner = owner,
                                             name = name,
                                             version = version,
                                             code = code,
                                             methods = methods,
                                             description = desc,
                                             signature = signature)

    val jsonString = userContract.asJson.spaces2
    info(jsonString)

    val r = for {
      json <- parse(jsonString)
      res  <- json.as[Contract.UserContract]
    } yield res

    assert(r.isRight)
  }
}
