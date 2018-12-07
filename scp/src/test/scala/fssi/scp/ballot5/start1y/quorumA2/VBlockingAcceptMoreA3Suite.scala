package fssi.scp.ballot5.start1y.quorumA2

import fssi.scp.ballot5.start1y.steps.VBlockingAcceptMoreA3
import org.scalatest.Matchers._

class VBlockingAcceptMoreA3Suite extends VBlockingAcceptMoreA3 {

  override def suiteName: String = "v-blocking accept more A3"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1Y()
    preparedA1()
    preparedA2()
    confirmPreparedA2()
    acceptCommit()
    quorumA2()
    quorumPreparedA3()

    vBlockingAcceptMoreA3()
  }

  test("Confirm A3") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 3, A3, cn = 2, hn = 3))

    app.numberOfEnvelopes shouldBe 9
    app.shouldHaveConfirmed(pn =3, A3, cn = 2, hn = 3)
    app.shouldBallotTimerFallBehind()
  }

  test("Externalize A3") {
    onEnvelopesFromVBlocking(makeExternalizeGen(A2, hn = 3))

    app.numberOfEnvelopes shouldBe 9
    app.shouldHaveConfirmed(pn = Int.MaxValue, AInf,  cn = 2, hn = Int.MaxValue)
    app.shouldNotHaveBallotTimer()
  }

  test("Other nodes confirm A4..A5") {
    onEnvelopesFromVBlocking(makeConfirmGen(pn = 3, A5, cn = 4, hn = 5))

    app.numberOfEnvelopes shouldBe 9
    app.shouldHaveConfirmed(pn = 3, A5, cn = 4, hn = 5)
    app.shouldNotHaveBallotTimer()
  }

  test("Other nodes externalize A4..A5") {
    onEnvelopesFromVBlocking(makeExternalizeGen(A4, hn = 5))

    app.numberOfEnvelopes shouldBe 9
    app.shouldHaveConfirmed(pn = Int.MaxValue, AInf, cn = 4, hn = Int.MaxValue)
    app.shouldNotHaveBallotTimer()
  }
}
