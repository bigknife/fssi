package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait QuorumPreparedA3 extends QuorumA2{
  def quorumPreparedA3(): Unit = {
    onEnvelopesFromVBlocking(makePrepareGen(A3, Some(A2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 7
    app.hasConfirmed(pn = 2, b = A3, cn = 2, hn = 2)
    app.hasBallotTimer shouldBe false

    onEnvelopesFromQuorumEx(makePrepareGen(A3, Some(A2), cn = 2, hn =2), checkTimers = true)

    app.numberOfEnvelopes shouldBe 8
    app.hasConfirmed(pn = 3, A3, cn = 2, hn = 2)
  }
}
