package fssi.scp.ballot5.normal1x
import fssi.scp.ballot5.normal1x.steps.NormalRound1X
import fssi.scp.types.Message.Confirm
import fssi.scp.types.{Ballot, Envelope}
import org.scalatest.Matchers._

class BumpToBallotSuite extends NormalRound1X {
  override def suiteName: String = "bumpToBallot prevented once committed"

  override def beforeEach(): Unit = {
    super.beforeEach()

    normalRound1X()
  }

  private def bumpToBallot(b2: Ballot): Unit = {
    val confirm1b2: Envelope[Confirm] =
      app.makeConfirm(node1, keyOfNode1, b2.counter, b2, b2.counter, b2.counter)
    val confirm2b2: Envelope[Confirm] =
      app.makeConfirm(node2, keyOfNode2, b2.counter, b2, b2.counter, b2.counter)
    val confirm3b2: Envelope[Confirm] =
      app.makeConfirm(node3, keyOfNode3, b2.counter, b2, b2.counter, b2.counter)
    val confirm4b2: Envelope[Confirm] =
      app.makeConfirm(node4, keyOfNode4, b2.counter, b2, b2.counter, b2.counter)

    app.onEnvelope(confirm1b2)
    app.onEnvelope(confirm2b2)
    app.onEnvelope(confirm3b2)
    app.onEnvelope(confirm4b2)

    app.numberOfEnvelopes shouldBe 5
    app.numberOfExternalizedValues shouldBe 1
  }

  test("by value") {
    bumpToBallot(Ballot(1, yValue))
  }

  test("by counter") {
    bumpToBallot(Ballot(2, xValue))
  }

  test("by value and counter") {
    bumpToBallot(Ballot(2, yValue))
  }
}
