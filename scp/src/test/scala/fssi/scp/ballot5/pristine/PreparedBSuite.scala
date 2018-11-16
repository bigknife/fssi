package fssi.scp.ballot5.pristine
import fssi.scp.ballot5.pristine.steps.PreparedB

import org.scalatest.Matchers._

class PreparedBSuite extends PreparedB{
override def suiteName: String = "prepared B"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedB()
  }

  test("v-blocking") {
    onEnvelopesFromVBlockingChecks(makePrepareGen(B1, Some(B1)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0
  }
}
