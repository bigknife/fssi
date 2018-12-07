package fssi.scp.ballot5.start1y.quorumA2

import fssi.scp.ballot5.start1y.steps.QuorumA2
import org.scalatest.Matchers._

class VBlockingSuite extends QuorumA2 {
  override def suiteName = "v-blocking"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    quorumA2()
  }

  test("prepared A3") {
    onEnvelopesFromVBlocking(makePrepareGen(A3, Some(A3), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 3, A3, cn = 2, hn = 2)
    app.shouldNotHaveBallotTimer()
  }

  test("prepared A3 + B3") {
    onEnvelopesFromVBlocking(makePrepareGen(A3, Some(A3), cn = 2, hn = 2, Some(B3)))

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 3, A3, cn = 2, hn = 2)
    app.shouldNotHaveBallotTimer()
  }

  test("confirm A3") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 3, A3, cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 3, A3, cn = 2, hn = 2)
    app.shouldNotHaveBallotTimer()
  }
}
