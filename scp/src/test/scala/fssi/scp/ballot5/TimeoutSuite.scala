package fssi.scp.ballot5
import fssi.scp.ballot5.steps.StepSpec
import fssi.scp.types.Ballot
import org.scalatest.Matchers._

class TimeoutSuite extends StepSpec {
  override def suiteName: String = "network edge cases"

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  test("timeout when h is set -> stay locked on h") {
    val bx: Ballot = Ballot(1, xValue)
    app.bumpState(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1

    // v-blocking -> prepared
    // quorum -> confirm prepared
    onEnvelopesFromQuorum(makePrepareGen(bx, Some(bx)))
    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(bx, Some(bx), cn = bx.counter, hn = bx.counter)

    // now, see if we can timeout and move to a different value
    app.bumpState(yValue) shouldBe true
    app.numberOfEnvelopes shouldBe 4
    val newbx: Ballot = Ballot(2, xValue)
    app.shouldHavePrepared(newbx, Some(bx), cn = bx.counter, hn = bx.counter)
  }

  test("timeout when h exists but can't be set -> vote for h") {
    // start with (1,y)
    val by: Ballot = Ballot(1, yValue)

    app.bumpState(yValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1

    val bx: Ballot = Ballot(1, xValue)
    // but quorum goes with (1,x)
    // v-blocking -> prepared
    onEnvelopesFromVBlocking(makePrepareGen(bx, Some(bx)))
    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(by, Some(bx))
    // quorum -> confirm prepared (no-op as b > h)
    onEnvelopesFromQuorumChecks(makePrepareGen(bx, Some(bx)),
                                checkEnvelopes = false,
                                isQuorumDelayed = false)
    app.numberOfEnvelopes shouldBe 2

    app.bumpState(yValue) shouldBe true
    app.numberOfEnvelopes shouldBe 3
    val newbx: Ballot = Ballot(2, xValue)
    // on timeout:
    // * we should move to the quorum's h value
    // * c can't be set yet as b > h
    app.shouldHavePrepared(newbx, Some(bx), cn = 0, hn = bx.counter)
  }

  test("timeout from multiple nodes") {
    app.bumpState(xValue) shouldBe true

    val x1: Ballot = Ballot(1, xValue)

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(x1)

    onEnvelopesFromQuorum(makePrepareGen(x1))
    // quorum -> prepared (1,x)
    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(x1, Some(x1))

    val x2: Ballot = Ballot(2, xValue)
    // timeout from local node
    app.bumpState(xValue) shouldBe true
    // prepares (2,x)
    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(x2, Some(x1))

    onEnvelopesFromQuorum(makePrepareGen(x1, Some(x1)))
    // quorum -> set nH=1
    app.numberOfEnvelopes shouldBe 4
    app.shouldHavePrepared(x2, Some(x1), cn = 0, hn = 1)

    onEnvelopesFromVBlocking(makePrepareGen(x2, Some(x2), cn = 1, hn = 1))
    // v-blocking prepared (2,x) -> prepared (2,x)
    app.numberOfEnvelopes shouldBe 5
    app.shouldHavePrepared(x2, Some(x2), cn = 0, hn = 1)

    onEnvelopesFromQuorum(makePrepareGen(x2, Some(x2), cn = 1, hn = 1))
    // quorum (including us) confirms (2,x) prepared -> set h=c=x2
    // we also get extra message: a quorum not including us confirms (1,x)
    // prepared
    //  -> we confirm c=h=x1
    app.numberOfEnvelopes shouldBe 7
    app.shouldHavePreparedAtIndex(index = 5, x2, Some(x2), cn = 2, hn = 2)
    app.shouldHaveConfirmed(pn = 2, x2, cn = 1, hn = 1)
  }

  test("timeout after prepare, receive old messages to prepare")
  {
    app.bumpState(xValue) shouldBe true

    val x1: Ballot = Ballot(1, xValue)

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(x1)

    app.onEnvelope(app.makePrepare(node1, keyOfNode1, x1))
    app.onEnvelope(app.makePrepare(node2, keyOfNode2, x1))
    app.onEnvelope(app.makePrepare(node3, keyOfNode3, x1))

    // quorum -> prepared (1,x)
    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(x1, Some(x1))

    val x2: Ballot = Ballot(2, xValue)
    // timeout from local node
    app.bumpState(xValue) shouldBe true
    // prepares (2,x)
    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(x2, Some(x1))

    val x3: Ballot = Ballot(3, xValue)
    // timeout again
    app.bumpState(xValue) shouldBe true
    // prepares (3,x)
    app.numberOfEnvelopes shouldBe 4
    app.shouldHavePrepared(x3, Some(x1))

    // other nodes moved on with x2
    app.onEnvelope(app.makePrepare(node1, keyOfNode1, x2, Some(x2), cn = 1, hn = 2))
    app.onEnvelope(app.makePrepare(node2, keyOfNode2, x2, Some(x2), cn = 1, hn = 2))
    // v-blocking -> prepared x2
    app.numberOfEnvelopes shouldBe 5
    app.shouldHavePrepared(x3, Some(x2))

    app.onEnvelope(app.makePrepare(node3, keyOfNode3, x2, Some(x2), cn = 1, hn = 2))
    // quorum -> set nH=2
    app.numberOfEnvelopes shouldBe 6
    app.shouldHavePrepared(x3, Some(x2), cn = 0, hn = 2)
  }
}
