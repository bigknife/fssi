package fssi.scp.ballot5.pristine.steps

import org.scalatest.Matchers._

trait ConfirmPreparedMixed extends PreparedA2{
  def confirmPreparedMixed(): Unit = {
    // a few nodes prepared A2
    // causes p=A2
    onEnvelopesFromVBlockingChecks(makePrepareGen(A2, Some(A2)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0

    // a few nodes prepared B2
    // causes p=B2, p'=A2
    onEnvelopesFromVBlockingChecks(makePrepareGen(A2, Some(B2), pPrime = Some(A2)), withChecks = false)

    app.numberOfEnvelopes shouldBe 0
  }
}
