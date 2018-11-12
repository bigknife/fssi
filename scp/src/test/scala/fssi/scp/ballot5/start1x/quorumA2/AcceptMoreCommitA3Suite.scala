package fssi.scp.ballot5.start1x.quorumA2

import fssi.scp.ballot5.start1x.steps.AcceptMoreCommitA3
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class AcceptMoreCommitA3Suite extends FunSuite with AcceptMoreCommitA3{
  override def suiteName: String = "Accept more commit A3"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    quorumA2()
    quorumPreparedA3()
    acceptMoreCommitA3()
  }

  test("Quorum externalize A3") {
    onEnvelopesFromQuorum(makeConfirmGen(pn = 3, A3, cn = 2, hn = 3))

    app.numberOfEnvelopes shouldBe 10
    app.hasExternalized(A2, hn = 3) shouldBe true
    app.hasBallotTimer shouldBe false
//
    app.numberOfExternalizedValues shouldBe 1
    app.lastExternalizedValue.contains(aValue) shouldBe true
  }
}
