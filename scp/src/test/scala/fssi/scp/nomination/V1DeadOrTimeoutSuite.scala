package fssi.scp.nomination
import fssi.scp.nomination.steps.V1DeadOrTimeout
import org.scalatest.FunSuite

import org.scalatest.Matchers._

class V1DeadOrTimeoutSuite extends FunSuite with V1DeadOrTimeout {
  override def suiteName: String = "v1 dead, timeout"

  override def beforeEach(): Unit = {
    super.beforeEach()

    v1IsTop()
    v1IsDeadOrTimeout()
  }

  test("v0 is new top node"){
    app.liftNodePriority(node0)
    app.nominate(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1
    app.hasNominated(votesX, emptyV)
  }

  test("v2 is new top node"){
    app.liftNodePriority(node2)
    app.nominate(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1
    app.hasNominated(votesX, emptyV)
  }

  test("v3 is new top node"){
    app.liftNodePriority(node3)
    app.nominate(xValue) shouldBe false
    app.numberOfEnvelopes shouldBe 0
  }
}
