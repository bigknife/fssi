package fssi
package types
package json

import org.scalatest._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import json.implicits._

class TransactionJsonCodecSpec extends FunSuite {
  test("decode transaction") {
    val jsonMessage = JsonMessage(
      "transaction",
      "{\"type\":\"Transfer\",\"body\":{\"id\":\"eb02994c-42a9-4d20-a178-d5d68112b6d2\",\"payer\":\"0x3059301306072a8648ce3d020106082a8648ce3d03010703420004c13463dfc9c328982b7e776c695d57b415b4bfc8ff5fa678eebac8af693ad2184bd743a71bc82064837807617fba36cc2e9d3e63cadbb0f716d28b9efee06b61\",\"payee\":\"0x1bced0\",\"token\":{\"amount\":0,\"tokenUnit\":\"Sweet\"},\"signature\":\"0x3045022100d91f617b7f06c3b34aebd185a823e78c365db66b8337e8b0a8c75f82eafed1e602207266452eb6df9f6b8137da953f6da9ae04b3a109d12057c1715484908b4625fd\",\"timestamp\":1534468957812}}"
    )
    val t = for {
      json        <- parse(jsonMessage.body)
      transaction <- json.as[Transaction]
    } yield transaction

    info(s"$t")
  }

  test("decode contract parameters") {
    /*
    import Contract.Parameter._
    val p1: Contract.Parameter = PString("Hello")
    info(p1.asJson.noSpaces)
    val p2: Contract.Parameter = PBigDecimal(new java.math.BigDecimal("100.0"))
    info(p2.asJson.noSpaces)
*/

    val s = "[100, true, false, 1.01E+3, 10.342567, \"hello\"]"
    val t= for {
      json <- parse(s)
      p <- json.as[Contract.Parameter]
    } yield p

    info(s"$t")

  }
}
