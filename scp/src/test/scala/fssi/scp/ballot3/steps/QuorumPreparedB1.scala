package fssi.scp.ballot3.steps

import org.scalatest.Matchers._

trait QuorumPreparedB1 extends PreparedB1 {
  def quorumPreparedB1(): Unit = {
    app.bumpTimerOffset()
    onEnvelopesFromQuorumChecks(makePrepareGen(B1, Some(B1)),
                                withChecks = false,
                                isQuorumDelayed = false)

    app.numberOfEnvelopes shouldBe 2
    // nothing happens:
    // computed_h = B1 (2)
    //    does not actually update h as b > computed_h
    //    also skips (3)
    app.shouldBallotTimerFallBehind()
  }
}
