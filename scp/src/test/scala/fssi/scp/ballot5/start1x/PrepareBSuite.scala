package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.steps.PrepareB

import org.scalatest.Matchers._

class PrepareBSuite extends PrepareB{
  override def suiteName: String = "prepare B"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    prepareB()
  }

  test("vBlocking") {
    onEnvelopesFromVBlocking(makePrepareGen(B1, Some(B1)))

    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(A1, Some(B1))
    app.shouldNotHaveBallotTimer()
  }

  test("quorum") {
    onEnvelopesFromQuorumChecksEx(makePrepareGen(B1), checkEnvelopes = true, isQuorumDelayed = true, checkTimers = true)

    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(A1, Some(B1))
  }
}
