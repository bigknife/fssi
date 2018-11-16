package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.steps.Switch

import org.scalatest.Matchers._

class SwitchSuite extends Switch {
  override def suiteName: String = "switch"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    preparedA1()
  }

  test("prepared B1") {
    onEnvelopesFromVBlocking(makePrepareGen(B1, Some(B1)))

    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(A1, Some(B1), cn = 0, hn = 0, Some(A1))
    app.shouldBallotTimerFallBehind()
  }

  test("prepare B1") {
    onEnvelopesFromQuorumChecks(makePrepareGen(B1), checkEnvelopes = true, isQuorumDelayed = true)

    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(A1, Some(B1), cn = 0, hn = 0, Some(A1))
    app.shouldBallotTimerFallBehind()
  }
}
