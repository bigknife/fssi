package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait PreparedA2 extends PreparedA1 {

  def preparedA2(): Unit = {
    app.bumpTimerOffset()

    app.bumpState(aValue) shouldBe true
    app.numberOfEnvelopes shouldBe 3
    app.shouldHavePrepared(A2, Some(A1))
    app.shouldNotHaveBallotTimer()

    onEnvelopesFromQuorumEx(makePrepareGen(A2), checkTimers = true)
    app.numberOfEnvelopes shouldBe 4
    app.shouldHavePrepared(A2, Some(A2))
  }
}
