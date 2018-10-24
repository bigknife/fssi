package fssi.scp

import org.scalatest._
import types._
import interpreter.store._

class InterpreterSpec extends FunSuite {
  ignore("var") {
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

  test("BallotStatus") {
    val nodeId = NodeID("hello".getBytes)
    val slotIndex = SlotIndex(1)

    val ballotStatus = BallotStatus.getInstance(nodeId, slotIndex)
    info(s"$ballotStatus")

    val ballotStatus1 = BallotStatus.getInstance(nodeId, slotIndex)
    ballotStatus1.phase := Ballot.Phase.Externalize

    info(s"$ballotStatus")
    info(s"$ballotStatus1")
    assert(ballotStatus1 == ballotStatus)
    assert(ballotStatus1.phase == ballotStatus.phase)

    ballotStatus.phase := Ballot.Phase.Confirm
    info(s"$ballotStatus")
    info(s"$ballotStatus1")
    assert(ballotStatus1 == ballotStatus)
    assert(ballotStatus1.phase == ballotStatus.phase)

    val slotIndex1 = SlotIndex(2)
    val ballotStatus3 = BallotStatus.getInstance(nodeId, slotIndex1)
    info(s"$ballotStatus3")
    assert(ballotStatus3 != ballotStatus)
  }
}
