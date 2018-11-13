package fssi.scp.ballot5.start1x.vblocking
import fssi.scp.ballot5.start1x.steps.VBlocking

import org.scalatest.Matchers._

class ExternalizeSuite extends VBlocking {
  override def suiteName: String = "Externalize"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    vBlocking()
  }

  test("EXTERNALIZE A2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(A2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.hasConfirmed(pn = Int.MaxValue, AInf, cn = 2, hn = Int.MaxValue) shouldBe true
    app.hasBallotTimer shouldBe false
  }

  test("EXTERNALIZE B2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(B2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.hasConfirmed(pn = Int.MaxValue, BInf, cn = 2, hn = Int.MaxValue) shouldBe true
    app.hasBallotTimer shouldBe false
  }
}
