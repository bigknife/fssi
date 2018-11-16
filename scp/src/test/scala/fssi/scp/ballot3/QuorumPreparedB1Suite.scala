package fssi.scp.ballot3
import fssi.scp.ballot3.steps.QuorumPreparedB1
import org.scalatest.Matchers._

class QuorumPreparedB1Suite extends QuorumPreparedB1 {
  override def suiteName: String = "quorum prepared B1"

  override def beforeEach(): Unit = {
    super.beforeEach()

    prepareBallot3TestBed()
    quorumVotesB1()
    quorumPreparedB1()
  }

  test("quorum bumps to A1") {
    app.bumpTimerOffset()
    onEnvelopesFromQuorumChecksEx2(makePrepareGen(A1, Some(B1)),
                                   withChecks = false,
                                   isQuorumDelayed = false,
                                   checkUpcoming = false,
                                   minQuorum = true)

    app.numberOfEnvelopes shouldBe 3
    // still does not set h as b > computed_h
    app.shouldHavePrepared(A1, Some(A1), cn = 0, hn = 0, pPrime = Some(B1))
    app.shouldBallotTimerFallBehind()

    app.bumpTimerOffset()
    // quorum commits A1
    onEnvelopesFromQuorumChecksEx2(makePrepareGen(A2, Some(A1), cn = 1, hn = 1, Some(B1)),
                                   withChecks = false,
                                   isQuorumDelayed = false,
                                   checkUpcoming = false,
                                   minQuorum = true)
    app.numberOfEnvelopes shouldBe 4
    app.shouldHaveConfirmed(pn = 2, A1, cn = 1, hn = 1)
    app.shouldBallotTimerFallBehind()
  }

}
