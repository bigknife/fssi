package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait ConfirmPreparedMixed extends PreparedA2{
  def confirmPreparedMixed(): Unit = {
    // a few nodes prepared B2
    onEnvelopesFromVBlocking(makePrepareGen(B2, Some(B2), cn = 0, hn = 0, Some(A2)))

    app.numberOfEnvelopes shouldBe 5
    app.shouldHavePrepared(A2,  Some(B2), cn = 0, hn = 0, Some(A2))
    app.shouldBallotTimerFallBehind()
  }
}
