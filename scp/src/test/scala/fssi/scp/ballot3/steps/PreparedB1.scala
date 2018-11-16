package fssi.scp.ballot3.steps

import org.scalatest.Matchers._

trait PreparedB1 extends Ballot3{

  def quorumVotesB1(): Unit = {
    app.bumpTimerOffset()
    onEnvelopesFromQuorumChecks(makePrepareGen(B1), withChecks = true, isQuorumDelayed = true)

    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(A1, Some(B1))
    app.shouldBallotTimerUpcoming()
  }
}
