package fssi.scp.ballot5.pristine
import fssi.scp.ballot5.pristine.steps.VBlocking

import org.scalatest.Matchers._

class VBConfirmSuite extends VBlocking{
  override def suiteName: String = "CONFIRM"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    VBlocking()
  }

  test("CONFIRM A2") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 2, A2, cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = 2, A2, cn = 2, hn = 2)
  }

  test("CONFIRM A3..4") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 4, A4, cn = 3, hn = 4))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = 4, A4, cn = 3, hn = 4)
  }

  test("CONFIRM B2") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 2, B2, cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = 2, B2, cn = 2, hn = 2)
  }
}
