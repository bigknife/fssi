package fssi.scp.nomination.steps
import fssi.scp.types.Envelope
import fssi.scp.types.Message.Nomination

import org.scalatest.Matchers._

trait WaitForV1 extends V1IsTop {

  val nom3: Envelope[Nomination] = app.makeNomination(node3, keyOfNode3, votesYZ, emptyV)
  val nom4: Envelope[Nomination] = app.makeNomination(node4, keyOfNode4, votesXZ, emptyV)

  def waitForV1(): Unit = {
    app.nominate(xValue) shouldBe false
    app.numberOfEnvelopes shouldBe 0

    // nothing happens with non top nodes
    app.onEnvelope(nom2)
    app.onEnvelope(nom3)
    app.numberOfEnvelopes shouldBe 0

    app.onEnvelope(nom1)
    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveNominated(votesY, emptyV)

    app.onEnvelope(nom4)
    app.numberOfEnvelopes shouldBe 1
  }
}
