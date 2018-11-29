package fssi.scp.nomination.steps
import fssi.scp.interpreter.store.Var
import fssi.scp.types.ValueSet

import org.scalatest.Matchers._

trait OthersNominateY extends StepSpec{
  val myVotes: Var[ValueSet] = Var(ValueSet.empty)
  val myAccepted: Var[ValueSet] = Var(ValueSet.empty)

  val votesY: Var[ValueSet] = Var(ValueSet.empty)
  val acceptedY: Var[ValueSet] = Var(ValueSet.empty)

  override def beforeEach(): Unit = {
    super.beforeEach()

    myVotes := ValueSet(xValue)
    myAccepted := ValueSet.empty

    votesY := ValueSet(yValue)
    acceptedY := ValueSet(yValue)
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  def selfNominateXAndOthersNominateY(): Unit = {
    app.forecastNomination(ValueSet(xValue), Some(xValue))
    app.nominate(xValue)

    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveNominated(myVotes.unsafe(), myAccepted.unsafe())
  }
}
