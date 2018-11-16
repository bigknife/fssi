package fssi.scp.ballot5.start1y.vblocking

import fssi.scp.ballot5.start1y.steps.VBlocking
import org.scalatest.Matchers._

class ConfirmSuite extends VBlocking{
  override def suiteName: String = "CONFIRM"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    vBlocking()
  }

  test("CONFIRM A2") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 2, A2, cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = 2, A2, cn = 2, hn = 2)
    app.shouldBallotTimerFallBehind()
  }

  test("CONFIRM A3..4") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 4, A4, cn = 3, hn = 4))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = 4, A4, cn = 3, hn = 4)
    app.shouldNotHaveBallotTimer()
  }

  test("CONFIRM B2") {
   onEnvelopesFromVBlocking(makeConfirmGen(pn = 2, B2, cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = 2, B2, cn = 2, hn = 2)
    app.shouldBallotTimerFallBehind()
  }
}
