package fssi.scp.ballot5.start1y.steps

import org.scalatest.Matchers._

trait ConfirmPreparedMixed extends PreparedA2{
  def confirmPreparedMixed(): Unit = {
    // a few nodes prepared B2
    onEnvelopesFromVBlocking(makePrepareGen(A2, Some(A2), cn = 0, hn = 0, Some(B2)))

    app.numberOfEnvelopes shouldBe 5
    app.shouldHavePrepared(A2,  Some(A2), cn = 0, hn = 0, Some(B2))
    app.shouldBallotTimerFallBehind()
  }
}
