package fssi.scp.nomination.steps
import fssi.scp.interpreter.store.Var
import fssi.scp.types.Message.Nomination
import fssi.scp.types.{Envelope, ValueSet}
import org.scalatest.Matchers._

trait OthersNominateWhatV0Nominates extends StepSpec {
  val votedValues: Var[ValueSet]    = Var(ValueSet.empty)
  val acceptedValues: Var[ValueSet] = Var(ValueSet.empty)

  def othersNominateWhatV0Nominate(): Unit = {
    require(app.nominate(xValue))

    votedValues := ValueSet(xValue)
    app.numberOfNominations shouldBe 1
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
    app.onEnvelope(nom1)
    app.onEnvelope(nom2)
    require(app.numberOfNominations == 1)

    // this cause 'x' be accepted (voted in a quorum)
    app.onEnvelope(nom3)
    require(app.numberOfNominations == 2)
    acceptedValues := ValueSet(xValue)
    app.hasNominated(votedValues.unsafe(), acceptedValues.unsafe()) shouldBe true
  }
}
