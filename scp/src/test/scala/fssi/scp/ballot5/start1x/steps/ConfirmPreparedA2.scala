package fssi.scp.ballot5.start1x.steps

import org.scalatest.Matchers._

trait ConfirmPreparedA2 extends PreparedA2{

  def confirmPreparedA2(): Unit = {
    onEnvelopesFromQuorum(makePrepareGen(A2, Some(A2)))

    app.numberOfEnvelopes shouldBe 5
    app.hasPrepared(A2, Some(A2), cn = 2, hn = 2)
    app.hasBallotTimerUpcoming shouldBe false
  }
}
