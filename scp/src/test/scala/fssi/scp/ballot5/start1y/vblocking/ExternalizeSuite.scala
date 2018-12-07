package fssi.scp.ballot5.start1y.vblocking

import fssi.scp.ballot5.start1y.steps.VBlocking
import org.scalatest.Matchers._

class ExternalizeSuite extends VBlocking {
  override def suiteName: String = "Externalize"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    vBlocking()
  }

  test("EXTERNALIZE A2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(A2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = Int.MaxValue, AInf, cn = 2, hn = Int.MaxValue)
    app.shouldNotHaveBallotTimer()
  }

  test("EXTERNALIZE B2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(B2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = Int.MaxValue, BInf, cn = 2, hn = Int.MaxValue)
    app.shouldNotHaveBallotTimer()
  }
}
