package fssi.scp.ballot5.start1y

import fssi.scp.ballot5.start1y.steps.Switch
import org.scalatest.Matchers._

class SwitchSuite extends Switch {
  override def suiteName: String = "switch"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
  }


  test("prepared B1") {
    onEnvelopesFromQuorumChecks(makePrepareGen(B1, Some(B1)), checkEnvelopes = false, isQuorumDelayed = false)

    app.numberOfEnvelopes shouldBe 2
    app.shouldBallotTimerFallBehind()
  }
}
