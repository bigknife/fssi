package fssi.scp.ballot5.start1y

import fssi.scp.ballot5.start1y.steps.ConflictingPreparedB
import org.scalatest.Matchers._

class ConflictingPreparedBSuite extends ConflictingPreparedB {
  override def suiteName: String = "get conflicting prepared B"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    conflictingPreparedB()
  }

  test("same counter") {
    // messages are ignored as B2 < A2
    onEnvelopesFromQuorumChecks(makePrepareGen(B2, Some(B2)),
                                checkEnvelopes = false,
                                isQuorumDelayed = false)

    app.numberOfEnvelopes shouldBe 5
    app.shouldBallotTimerFallBehind()
  }

  test("higher counter") {
    onEnvelopesFromVBlocking(makePrepareGen(B3, Some(B2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    // A2 > B2 -> p = A2, p'=B2
    // TODO: in original case cn assumed to be 2

    app.shouldHavePrepared(A3, Some(A2), cn = 2, hn = 2, Some(B2))
//    app.shouldHavePrepared(A3, Some(A2), cn = 0, hn = 2, Some(B2))
    app.shouldNotHaveBallotTimer()

    // node is trying to commit A2=<2,y> but rest
    // of its quorum is trying to commit B2
    // we end up with a delayed quorum
    onEnvelopesFromQuorumChecksEx(makePrepareGen(B3, Some(B2), cn = 2, hn = 2),
                                  checkEnvelopes = true,
                                  isQuorumDelayed = true,
                                  checkTimers = true)

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 3, B3, cn = 2, hn = 2)
  }
}
