package fssi.scp.ballot5.pristine

import fssi.scp.ballot5.pristine.steps.VBlocking

import org.scalatest.Matchers._

class VBExternalizeSuite extends VBlocking{
  override def suiteName: String = "EXTERNALIZE"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    VBlocking()
  }

  test("EXTERNALIZE A2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(A2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = Int.MaxValue, AInf, cn = 2, hn = Int.MaxValue)
  }

  test("EXTERNALIZE B2") {
    onEnvelopesFromVBlocking(makeExternalizeGen(B2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = Int.MaxValue, BInf, cn = 2, hn = Int.MaxValue)
  }

}
