package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.steps.ConflictingPreparedB

import org.scalatest.Matchers._

class ConflictingPreparedBSuite extends ConflictingPreparedB{
  override def suiteName: String = "get conflicting prepared B"

override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    conflictingPreparedB()
  }

  test("same counter") {
    onEnvelopesFromVBlocking(makePrepareGen(B2, Some(B2)))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHavePrepared(A2, Some(B2), cn = 0, hn = 2, Some(A2))
    app.shouldBallotTimerFallBehind()

    onEnvelopesFromQuorum(makePrepareGen(B2, Some(B2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 2, B2, cn =2, hn =2)
    app.shouldBallotTimerFallBehind()
  }

  test("higher counter") {
    onEnvelopesFromVBlocking(makePrepareGen(B3, Some(B2), cn = 2, hn = 2))

    app.numberOfEnvelopes shouldBe 6
    app.shouldHavePrepared(A3, Some(B2), cn = 0, hn = 2, Some(A2))
    app.shouldNotHaveBallotTimer()

    onEnvelopesFromQuorumChecksEx(makePrepareGen(B3, Some(B2), cn = 2, hn = 2), checkEnvelopes = true, isQuorumDelayed = true, checkTimers = true)

    app.numberOfEnvelopes shouldBe 7
    app.shouldHaveConfirmed(pn = 3, B3, cn =2, hn =2)
  }
}
