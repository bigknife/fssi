package fssi.scp.ballot5.pristine.steps

import org.scalatest.Matchers._

trait PreparedA1 extends StartFromPristine {
  def preparedA1(): Unit = {
    onEnvelopesFromQuorumChecks(makePrepareGen(A1), checkEnvelopes = false, isQuorumDelayed = false)

    app.numberOfEnvelopes shouldBe 0
  }
}
