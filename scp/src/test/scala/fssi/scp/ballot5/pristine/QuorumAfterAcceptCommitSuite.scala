package fssi.scp.ballot5.pristine
import fssi.scp.ballot5.pristine.steps.AcceptCommit

import org.scalatest.Matchers._

class QuorumAfterAcceptCommitSuite extends AcceptCommit {
  override def suiteName: String = "quorum"

  override def beforeEach(): Unit = {
    super.beforeEach()

    startFromPristine()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
  }
  test("Quorum A2") {
    onEnvelopesFromQuorum(makePrepareGen(A2, Some(A2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = 2, A2, cn = 2, hn = 2)
  }

  test("Quorum B2") {
    onEnvelopesFromQuorum(makePrepareGen(B2, Some(B2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveConfirmed(pn = 2, B2, cn = 2, hn = 2)
  }
}
