package fssi.scp.ballot5.start1y

import fssi.scp.ballot5.start1y.steps.PrepareB
import org.scalatest.Matchers._

class PrepareBSuite extends PrepareB{
  override def suiteName: String = "prepare B"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    prepareB()
  }

  test("v-blocking") {
    onEnvelopesFromVBlocking(makePrepareGen(B1, Some(B1)))

    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(A1, Some(B1))
    app.shouldNotHaveBallotTimer()
  }

}
