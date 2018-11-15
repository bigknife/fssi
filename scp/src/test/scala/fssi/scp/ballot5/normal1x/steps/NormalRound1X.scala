package fssi.scp.ballot5.normal1x.steps
import fssi.scp.ballot5.steps.StepSpec
import fssi.scp.types.Message.{Confirm, Prepare}
import fssi.scp.types.{Ballot, Envelope}
import org.scalatest.Matchers._

trait NormalRound1X extends StepSpec {
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

// bunch of confirm messages
  val confirm1: Envelope[Confirm] =
    app.makeConfirm(node1, keyOfNode1, pn = b.counter, b, cn = b.counter, hn = b.counter)
  val confirm2: Envelope[Confirm] =
    app.makeConfirm(node2, keyOfNode2, pn = b.counter, b, cn = b.counter, hn = b.counter)
  val confirm3: Envelope[Confirm] =
    app.makeConfirm(node3, keyOfNode3, pn = b.counter, b, cn = b.counter, hn = b.counter)
  val confirm4: Envelope[Confirm] =
    app.makeConfirm(node4, keyOfNode4, pn = b.counter, b, cn = b.counter, hn = b.counter)

  def normalRound1X(): Unit = {
    nodesAllPledgeToCommit()
    app.numberOfEnvelopes shouldBe 3

    // those should not trigger anything just yet
    app.onEnvelope(preparedC1)
    app.onEnvelope(preparedC2)
    app.numberOfEnvelopes shouldBe 3

    // this should cause the node to accept 'commit b' (quorum)
    // and therefore send a "CONFIRM" message
    app.onEnvelope(preparedC3)
    app.numberOfEnvelopes shouldBe 4
    app.shouldHaveConfirmed(pn = 1, b, cn = b.counter, hn = b.counter)

    // those should not trigger anything just yet
    app.onEnvelope(confirm1)
    app.onEnvelope(confirm2)
    app.numberOfEnvelopes shouldBe 4

    app.onEnvelope(confirm3)
    // this causes our node to
    // externalize (confirm commit c)
    app.numberOfEnvelopes shouldBe 5

    // The slot should have externalized the value
    app.numberOfExternalizedValues shouldBe 1
    app.lastExternalizedValue shouldBe Some(xValue)
    app.shouldHaveExternalized(b, b.counter)

    // extra vote should not do anything
    app.onEnvelope(confirm4)
    app.numberOfEnvelopes shouldBe 5
    app.numberOfExternalizedValues shouldBe 1

    // duplicate should just no-op
    app.onEnvelope(confirm2)
    app.numberOfEnvelopes shouldBe 5
    app.numberOfExternalizedValues shouldBe 1
  }
}
