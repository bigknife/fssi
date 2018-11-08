package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait AcceptMoreCommitA3 extends QuorumPreparedA3{
  def acceptMoreCommitA3(): Unit = {
    onEnvelopesFromQuorum(makePrepareGen(A3, Some(A3), cn = 2, hn = 3))

    app.numberOfEnvelopes shouldBe 9
    app.hasConfirmed(pn = 3, A3, cn = 2, hn = 3)
    app.hasBallotTimerUpcoming shouldBe false
    app.numberOfExternalizedValues shouldBe 0
  }
}
