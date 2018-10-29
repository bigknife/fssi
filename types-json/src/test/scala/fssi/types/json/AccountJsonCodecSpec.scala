package fssi
package types
package json
import fssi.types.biz.Account
import org.scalatest.FunSuite
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import fssi.types.json.implicits._

class AccountJsonCodecSpec extends FunSuite {

  test("test account json coder") {
    val privKey = Account.PrivKey("priv".getBytes)
    val pubKey  = Account.PubKey("pub".getBytes)
    val iv      = Account.IV("iv".getBytes)
    val id      = Account.ID("id".getBytes)
    val account = Account(privKey, pubKey, iv, id)

    val jsonString = account.asJson.spaces2
    info(jsonString)

    val r = for {
      json <- parse(jsonString)
      res  <- json.as[Account]
    } yield res

    assert(r.isRight)
    val decodeAccount = r.right.get
    assert(decodeAccount.encPrivKey.value.sameElements(privKey.value))
    assert(decodeAccount.pubKey.value.sameElements(pubKey.value))
    assert(decodeAccount.iv.value.sameElements(iv.value))
    assert(decodeAccount.id.value.sameElements(id.value))
  }
}
