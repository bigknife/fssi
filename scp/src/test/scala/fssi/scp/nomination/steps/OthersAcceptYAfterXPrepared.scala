package fssi.scp.nomination.steps
import fssi.scp.types.Envelope
import fssi.scp.types.Message.Nomination

import org.scalatest.Matchers._

trait OthersAcceptYAfterXPrepared extends StepSpec {
  def thenOthersAcceptY(): Unit = {

    val acc1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, anotherVotedValues.unsafe(), anotherVotedValues.unsafe())
    val acc2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, anotherVotedValues.unsafe(), anotherVotedValues.unsafe())
    val acc3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, anotherVotedValues.unsafe(), anotherVotedValues.unsafe())
    val acc4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, anotherVotedValues.unsafe(), anotherVotedValues.unsafe())

    app.onEnvelope(acc1)
    app.numberOfEnvelopes shouldBe 3

    // v-blocking
    app.onEnvelope(acc2)
    app.numberOfEnvelopes shouldBe 4
    app.hasNominated(anotherVotedValues.unsafe(), anotherVotedValues.unsafe()) shouldBe true

    app.forecastNomination(anotherVotedValues.unsafe(), Some(zValue))

    // this updates the composite value to use next time
    // but does not prepare it
    app.onEnvelope(acc3)
    app.numberOfEnvelopes shouldBe 4
    app.latestCompositeCandidateValue shouldBe Some(zValue)

    app.onEnvelope(acc4)
    app.numberOfEnvelopes shouldBe 4
  }
}
