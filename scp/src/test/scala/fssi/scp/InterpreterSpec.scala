package fssi.scp

import org.scalatest._

import interpreter.store._

class InterpreterSpec extends FunSuite {
  test("var") {
    val x: Var[Int] = Var(1)
    info(s"x = $x")
    x := 2
    info(s"x = ${x()}")

    val y = x.map(_ * 2)
    info(s"y = $y")

    val z = for {
      a <- x
      b <- y
    } yield a + b

    info(s"z = $z")

    val nominationStatus = NominationStatus(Var(1))
    info(s"nom status: $nominationStatus")

    nominationStatus.roundNumber := 2
    info(s"nom status: $nominationStatus")

    val xi = x.unsafe()
    info(s"xi = $xi")

  }
}
