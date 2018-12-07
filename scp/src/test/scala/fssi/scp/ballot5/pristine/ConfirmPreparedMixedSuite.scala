package fssi.scp.ballot5.pristine
import fssi.scp.ballot5.pristine.steps.ConfirmPreparedMixed

import org.scalatest.Matchers._

class ConfirmPreparedMixedSuite extends ConfirmPreparedMixed {
  override def suiteName: String = "Confirm prepared mixed"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
    preparedA2()
    confirmPreparedMixed()
  }

  test("mixed A2") {
    // causes h=A2
    // but c = 0, as p >!~ h
    app.onEnvelope(app.makePrepare(node3, keyOfNode3, A2, Some(A2)))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(A2, Some(B2), cn = 0, hn = 2, Some(A2))

    app.onEnvelope(app.makePrepare(node4, keyOfNode4, A2, Some(A2)))

    app.numberOfEnvelopes shouldBe 1
  }

  test("mixed B2") {
    // causes h=B2, c=B2
    app.onEnvelope(app.makePrepare(node3, keyOfNode3, B2, Some(B2)))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(B2, Some(B2), cn = 2, hn = 2, Some(A2))

    app.onEnvelope(app.makePrepare(node4, keyOfNode4, A2, Some(A2)))
  }
}
