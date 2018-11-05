package fssi.scp.nomination.steps
import fssi.scp.types.Message.Nomination
import fssi.scp.types.{Ballot, Envelope, ValueSet}
import org.scalatest.Matchers._

trait OthersNominateWhatV0Nominates extends StepSpec {

  def othersNominateWhatV0Nominate(): Unit = {
    require(app.nominate(xValue))

    votedValues := ValueSet(xValue)
    app.numberOfEnvelopes shouldBe 1
    app.hasNominated(votedValues.unsafe(), acceptedValues.unsafe()) shouldBe true

    val nom1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, votedValues.unsafe(), acceptedValues.unsafe())
    val nom2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, votedValues.unsafe(), acceptedValues.unsafe())
    val nom3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, votedValues.unsafe(), acceptedValues.unsafe())
    val nom4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, votedValues.unsafe(), acceptedValues.unsafe())

    // nothing happened yet
    require(app.onEnvelope(nom1))
    require(app.onEnvelope(nom2))
    app.numberOfEnvelopes shouldBe 1

    // this cause 'x' be accepted (voted in a quorum)
    require(app.onEnvelope(nom3))
    app.numberOfEnvelopes shouldBe 2
    acceptedValues := ValueSet(xValue)
    app.hasNominated(votedValues.unsafe(), acceptedValues.unsafe()) shouldBe true

    // extra message doesn't do anything
    app.onEnvelope(nom4)
    app.numberOfEnvelopes shouldBe 2

    val acc1: Envelope[Nomination] =
      app.makeNomination(node1, keyOfNode1, votedValues.unsafe(), acceptedValues.unsafe())
    val acc2: Envelope[Nomination] =
      app.makeNomination(node2, keyOfNode2, votedValues.unsafe(), acceptedValues.unsafe())
    val acc3: Envelope[Nomination] =
      app.makeNomination(node3, keyOfNode3, votedValues.unsafe(), acceptedValues.unsafe())
    val acc4: Envelope[Nomination] =
      app.makeNomination(node4, keyOfNode4, votedValues.unsafe(), acceptedValues.unsafe())

    // nothing happens
    app.onEnvelope(acc1)
    app.onEnvelope(acc2)
    app.numberOfEnvelopes shouldBe 2

    app.forecastNomination(ValueSet(xValue), Some(xValue))
    // this causes the node to send a prepare message (quorum)
    app.onEnvelope(acc3)
    app.numberOfEnvelopes shouldBe 3
    app.hasPrepared(Ballot(1, xValue))

    app.onEnvelope(acc4)
    app.numberOfEnvelopes shouldBe 3

    anotherVotedValues := ValueSet(xValue, yValue)
  }
}
