package fssi
package parser

import org.scalatest._
import sql._

class ParsersSpec extends FunSuite {

  test("parser law") {
    val law = Parsers.Law(sqlparsers)
    assert(law.charRule('A'))
    info("passed char rule")
    assert(law.stringRule("hello,world"))
    info("passed string rule")
  }

  test("or parser") {
    import sqlparsers.{run => prun, _}
    assert(prun("hello" | "world")("world").isRight)
    assert(prun("hello" | "world")("world1").isLeft)
  }
}
