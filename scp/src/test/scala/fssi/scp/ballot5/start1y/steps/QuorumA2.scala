package fssi.scp.ballot5.start1y.steps

import org.scalatest.Matchers._

trait QuorumA2 extends AcceptCommit {
  def quorumA2(): Unit = {
    onEnvelopesFromQuorum(makePrepareGen(A2, Some(A2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHaveConfirmed(pn = 2, A2, cn = 2, hn = 2)
    app.shouldBallotTimerFallBehind()
  }
}
