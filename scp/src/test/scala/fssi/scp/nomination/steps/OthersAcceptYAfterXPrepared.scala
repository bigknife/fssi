package fssi.scp.nomination.steps
import fssi.scp.types.Envelope
import fssi.scp.types.Message.Nomination

import org.scalatest.Matchers._

trait OthersAcceptYAfterXPrepared extends StepSpec {
  def thenOthersAcceptY(): Unit = {

    val acc1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, votes2.unsafe(), votes2.unsafe())
    val acc2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, votes2.unsafe(), votes2.unsafe())
    val acc3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, votes2.unsafe(), votes2.unsafe())
    val acc4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, votes2.unsafe(), votes2.unsafe())

    app.onEnvelope(acc1)
    app.numberOfEnvelopes shouldBe 3

    // v-blocking
    app.onEnvelope(acc2)
    app.numberOfEnvelopes shouldBe 4
    app.hasNominated(votes2.unsafe(), votes2.unsafe()) shouldBe true

    app.forecastNomination(votes2.unsafe(), Some(zValue))

    // this updates the composite value to use next time
    // but does not prepare it
    app.onEnvelope(acc3)
    app.numberOfEnvelopes shouldBe 4
    app.latestCompositeCandidateValue shouldBe Some(zValue)

    app.onEnvelope(acc4)
    app.numberOfEnvelopes shouldBe 4
  }
}
