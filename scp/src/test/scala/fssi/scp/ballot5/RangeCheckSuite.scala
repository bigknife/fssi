package fssi.scp.ballot5
import fssi.scp.ballot5.steps.StepSpec
import fssi.scp.types.Message.{Confirm, Prepare}
import fssi.scp.types.{Ballot, Envelope}
import org.scalatest.Matchers._

class RangeCheckSuite extends StepSpec {
  override def suiteName: String = "range check"

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  test("range check") {
    nodesAllPledgeToCommit()
    app.numberOfEnvelopes shouldBe 3

    val b: Ballot = Ballot(1, xValue)

    // bunch of prepare messages with "commit b"
    val preparedC1: Envelope[Prepare] =
      app.makePrepare(node1, keyOfNode1, b, Some(b), cn = b.counter, hn = b.counter)
    val preparedC2: Envelope[Prepare] =
      app.makePrepare(node2, keyOfNode2, b, Some(b), cn = b.counter, hn = b.counter)
    val preparedC3: Envelope[Prepare] =
      app.makePrepare(node3, keyOfNode3, b, Some(b), cn = b.counter, hn = b.counter)
    val preparedC4: Envelope[Prepare] =
      app.makePrepare(node4, keyOfNode4, b, Some(b), cn = b.counter, hn = b.counter)

    // those should not trigger anything just yet
    app.onEnvelope(preparedC1)
    app.onEnvelope(preparedC2)
    app.numberOfEnvelopes shouldBe 3

    // this should cause the node to accept 'commit b' (quorum)
    // and therefore send a "CONFIRM" message
    app.onEnvelope(preparedC3)
    app.numberOfEnvelopes shouldBe 4
    app.shouldHaveConfirmed(pn = 1, b, cn = b.counter, hn = b.counter)

    // bunch of confirm messages with different ranges
    val confirm1: Envelope[Confirm] =
      app.makeConfirm(node1, keyOfNode1, pn = 4, Ballot(4, xValue), cn = 2, hn = 4)
    val confirm2: Envelope[Confirm] =
      app.makeConfirm(node2, keyOfNode2, pn = 6, Ballot(6, xValue), cn = 2, hn = 6)
    val confirm3: Envelope[Confirm] =
      app.makeConfirm(node3, keyOfNode3, pn = 5, Ballot(5, xValue), cn = 3, hn = 5)
    val confirm4: Envelope[Confirm] =
      app.makeConfirm(node4, keyOfNode4, pn = 6, Ballot(6, xValue), cn = 3, hn = 6)

    // this should not trigger anything just yet
    app.onEnvelope(confirm1)

    // v-blocking
    //   * b gets bumped to (4,x)
    //   * p gets bumped to (4,x)
    //   * (c,h) gets bumped to (2,4)
    app.onEnvelope(confirm2)
    app.numberOfEnvelopes shouldBe 5
    app.shouldHaveConfirmed(pn = 4, Ballot(4, xValue), cn = 2, hn = 4)

    // this causes to externalize
    // range is [3,4]
    app.onEnvelope(confirm4)
    app.numberOfEnvelopes shouldBe 6

    // The slot should have externalized the value
    app.numberOfExternalizedValues shouldBe 1
    app.lastExternalizedValue shouldBe Some(xValue)
    app.shouldHaveExternalized(Ballot(3, xValue), hn = 4)
  }
}
