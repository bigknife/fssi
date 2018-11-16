package fssi.scp.ballot5
import fssi.scp.ballot5.steps.TestBed
import fssi.scp.types.Ballot
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class BumpStateXSuite extends FunSuite with TestBed{
  override def suiteName: String = "bumpState X"

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  test("bump to state x"){
    app.bumpState(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1

    app.shouldHavePrepared(Ballot(1, xValue))
  }
}
