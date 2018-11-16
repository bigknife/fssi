package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.steps.ConfirmVBlocking

import org.scalatest.Matchers._

class ConfirmVBlockingSuite extends ConfirmVBlocking {
  override def suiteName: String = "confirm (v-blocking)"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    confirmVBlocking()
  }

  test("via CONFIRM") {
    app.bumpTimerOffset()
    app.onEnvelope(app.makeConfirm(node1, keyOfNode1, pn = 3, A3, cn = 3, hn = 3))
    app.onEnvelope(app.makeConfirm(node2, keyOfNode2, pn = 4, A4, cn = 2, hn = 4))

    app.numberOfEnvelopes shouldBe 2
    app.shouldHaveConfirmed(pn = 3, A3, cn = 3, hn = 3)
    app.shouldNotHaveBallotTimer()
  }

  test("via EXTERNALIZE") {
    app.onEnvelope(app.makeExternalize(node1, keyOfNode1, A2, hn = 4))
    app.onEnvelope(app.makeExternalize(node2, keyOfNode2, A3, hn = 5))

    app.numberOfEnvelopes shouldBe 2
    app.shouldHaveConfirmed(pn = Int.MaxValue, AInf, cn = 3, hn = Int.MaxValue)
    app.shouldNotHaveBallotTimer()
  }
}
