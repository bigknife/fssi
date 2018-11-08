package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait PreparedA2 extends PreparedA1 {

  def preparedA2(): Unit = {
    app.bumpTimeOffset()

    app.bumpState(aValue) shouldBe true
    app.numberOfEnvelopes shouldBe 3
    app.hasPrepared(A2, Some(A1))
    app.hasBallotTimer shouldBe false

    onEnvelopesFromQuorumEx(makePrepareGen(A2), checkTimers = true)
    app.numberOfEnvelopes shouldBe 4
    app.hasPrepared(A2, Some(A2))
  }
}
