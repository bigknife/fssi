package fssi.scp.nomination.steps

import org.scalatest.Matchers._

trait V1DeadOrTimeout extends V1IsTop{

  def v1IsDeadOrTimeout(): Unit = {
    app.nominate(xValue) shouldBe false
    app.numberOfEnvelopes shouldBe 0

    app.onEnvelope(nom2)
    app.numberOfEnvelopes shouldBe 0
  }
}
