package fssi
package types
package json
import fssi.types.biz.{Account, Node}
import org.scalatest.FunSuite
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import json.implicits._

class NodeJsonCodecSpec extends FunSuite {

  test("test node json coder") {
    /*
    val address    = Node.Addr("local", 80)
    val priKey     = Account.PrivKey("priKey".getBytes())
    val pubKey     = Account.PubKey("pubKey".getBytes())
    val iv         = Account.IV("iv".getBytes())
    val id         = Account.ID("id".getBytes())
    val account    = Account(priKey, pubKey, iv, id)
    val node       = Node(address = address, account = account)
    val jsonString = node.asJson.spaces2
    info(jsonString)

    val r = for {
      json <- parse(jsonString)
      res  <- json.as[Node]
    } yield res

    assert(r.isRight)
    */
  }
}
