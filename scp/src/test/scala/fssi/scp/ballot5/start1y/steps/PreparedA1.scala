package fssi.scp.ballot5.start1y.steps

import org.scalatest.Matchers._

trait PreparedA1 extends Start1Y {
  def preparedA1(): Unit = {
    onEnvelopesFromQuorumEx(makePrepareGen(A1), checkTimers = true)

    app.numberOfEnvelopes shouldBe 2
    app.shouldHavePrepared(A1, Some(A1))
  }
}
