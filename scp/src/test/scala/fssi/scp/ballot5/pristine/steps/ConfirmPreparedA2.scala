package fssi.scp.ballot5.pristine.steps

import org.scalatest.Matchers._

trait ConfirmPreparedA2 extends PreparedA2 {
  def confirmPreparedA2(): Unit = {
    onEnvelopesFromVBlockingChecks(makePrepareGen(A2, Some(A2)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0
  }
}
