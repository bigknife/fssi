package fssi.scp.nomination.steps
import fssi.scp.types.Message.Nomination
import fssi.scp.types.{Envelope, ValueSet}
import org.scalatest.Matchers._

trait RestoreNomination extends StepSpec {


  def restoreNomination(): Unit = {
    app2.reset()

    app2.setStatusFromMessage(Nomination(votes.unsafe(), ValueSet(xValue)))

    app2.nominate(yValue)
    app2.numberOfEnvelopes shouldBe 1
    app2.hasNominated(votes2.unsafe(), ValueSet(xValue)) shouldBe true

    val nom1: Envelope[Nomination] =
      app2.makeNomination(node1, keyOfNode1, ValueSet(xValue), ValueSet.empty)
    val nom2: Envelope[Nomination] =
      app2.makeNomination(node2, keyOfNode2, ValueSet(xValue), ValueSet.empty)
    val nom3: Envelope[Nomination] =
      app2.makeNomination(node3, keyOfNode3, ValueSet(xValue), ValueSet.empty)
    val nom4: Envelope[Nomination] =
      app2.makeNomination(node4, keyOfNode4, ValueSet(xValue), ValueSet.empty)

    app2.onEnvelope(nom1)
    app2.onEnvelope(nom2)

    // 'x' is accepted (quorum)
    // but because the restored state already included
    // 'x' in the accepted set, no new message is emitted
    app2.onEnvelope(nom3)

    app2.forecastNomination(ValueSet(xValue), Some(xValue))

    val acc1: Envelope[Nomination] =
      app2.makeNomination(node1, keyOfNode1, ValueSet(xValue), ValueSet(xValue))
    val acc2: Envelope[Nomination] =
      app2.makeNomination(node2, keyOfNode2, ValueSet(xValue), ValueSet(xValue))
    val acc3: Envelope[Nomination] =
      app2.makeNomination(node3, keyOfNode3, ValueSet(xValue), ValueSet(xValue))
    val acc4: Envelope[Nomination] =
      app2.makeNomination(node4, keyOfNode4, ValueSet(xValue), ValueSet(xValue))

    // other nodes not emit 'x' as accepted
    app2.onEnvelope(acc1)
    app2.onEnvelope(acc2)
    app2.numberOfEnvelopes shouldBe 1

    // this causes the node to update its composite value to x
    app2.onEnvelope(acc3)
  }
}
