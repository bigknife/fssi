package fssi.scp

import org.scalatest._
import types._
import interpreter.store._

import scala.collection.immutable.TreeSet

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

    val nominationStatus = NominationStatus.empty
    info(s"nom status: $nominationStatus")

    nominationStatus.roundNumber := 2
    info(s"nom status: $nominationStatus")

    val xi = x.unsafe()
    info(s"xi = $xi")

  }

  test("BallotStatus") {
    val nodeId    = NodeID("hello".getBytes)
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

    val slotIndex1    = SlotIndex(2)
    val ballotStatus3 = BallotStatus.getInstance(nodeId, slotIndex1)
    info(s"$ballotStatus3")
    assert(ballotStatus3 != ballotStatus)
  }

  test("NominationStatus") {
    val nodeId    = NodeID("nominate".getBytes)
    val slotIndex = SlotIndex(1)

    val initStatus = NominationStatus.getInstance(nodeId, slotIndex)
    info(s"$initStatus")

    val status1 = NominationStatus.getInstance(nodeId, slotIndex)
    status1.roundNumber := 2
    info(s"$initStatus")
    info(s"$status1")
    assert(initStatus == status1)

    val status2 = NominationStatus.getInstance(nodeId, slotIndex)
    val value1  = TestValue(TreeSet(10))
    val votes   = TreeSet[Value](value1)
    status2.votes := votes
    info(s"$initStatus")
    info(s"$status1")
    info(s"$status2")
    assert(status2 == status1)
    assert(status1 == initStatus)
    assert(status2.votes.unsafe() == votes)
  }
}
