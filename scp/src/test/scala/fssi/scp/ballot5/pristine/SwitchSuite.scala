package fssi.scp.ballot5.pristine
import fssi.scp.ballot5.pristine.steps.Switch

import org.scalatest.Matchers._

class SwitchSuite extends Switch {
  override def suiteName: String = "switch"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
  }

  test("switch prepared B1") {
    onEnvelopesFromVBlockingChecks(makePrepareGen(B1, Some(B1)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0
  }
}
