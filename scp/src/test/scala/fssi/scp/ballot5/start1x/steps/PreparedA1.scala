package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait PreparedA1 extends Start1X {
  def preparedA1(): Unit = {

    onEnvelopesFromQuorumEx(makePrepareGen(A1), checkTimers = true)

    app.numberOfEnvelopes shouldBe 2
    app.hasPrepared(A1, Some(A1))
  }
}
