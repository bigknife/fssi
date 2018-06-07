package fssi.interpreter

import org.scalatest.FunSuite
import io.circe.parser._
import io.circe.syntax._
import fssi.interpreter.jsonCodec._
import fssi.ast.domain.types._

class JsonCodecSpec extends FunSuite {
  test("Contract.Parameter") {
    val s = "[\"name\",1]"

    val p = parse(s).map(_.as[Contract.Parameter])
    info(s"$p")
  }
}
