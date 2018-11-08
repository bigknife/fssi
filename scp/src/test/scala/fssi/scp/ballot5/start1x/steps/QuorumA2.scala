package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait QuorumA2 extends AcceptCommit{

  def quorumA2(): Unit = {
    onEnvelopesFromQuorum(makePrepareGen(A2, Some(A2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.hasConfirmed(pn = 2, b = A2, cn = 2, hn = 2)
    app.hasBallotTimerUpcoming shouldBe false
  }
}
