package fssi.scp.nomination
import fssi.scp.nomination.steps.WaitForV1
import fssi.scp.types.ValueSet
import org.scalatest.FunSuite

import org.scalatest.Matchers._

class WaitForV1Suite extends FunSuite with WaitForV1 {
  override def suiteName: String = "nomination wait for v1"

  override def beforeEach(): Unit = {
    super.beforeEach()

    v1IsTop()
    waitForV1()
  }

  test("timeout -> pick another value from v1") {
    app.forecastNomination(ValueSet(xValue), Some(xValue))

    // note: value passed in here should be ignored
    app.nominate(zValue) shouldBe true
    // picks up 'x' from v1 (as we already have 'y')
    // which also happens to causes 'x' to be accepted
    app.numberOfEnvelopes shouldBe 2
    app.shouldHaveNominated(votesXY, votesX)
  }
}
