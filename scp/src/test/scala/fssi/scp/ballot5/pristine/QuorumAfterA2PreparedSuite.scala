package fssi.scp.ballot5.pristine
import fssi.scp
import fssi.scp.ballot5.pristine.steps.ConfirmPreparedA2

import org.scalatest.Matchers._

class QuorumAfterA2PreparedSuite extends ConfirmPreparedA2 {
  override def suiteName: String = "quorum"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
  }

  test("Quorum A2") {
    onEnvelopesFromVBlockingChecks(makePrepareGen(A2, Some(A2)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0

    onEnvelopesFromQuorum(makePrepareGen(A2, Some(A2)))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(A2, Some(A2), cn = 1, hn = 2)
  }

  test("Quorum B2") {
    onEnvelopesFromVBlockingChecks(makePrepareGen(B2, Some(B2)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0

    onEnvelopesFromQuorum(makePrepareGen(B2, Some(B2)))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(B2, Some(B2), cn = 2, hn = 2, Some(A2))
  }
}
