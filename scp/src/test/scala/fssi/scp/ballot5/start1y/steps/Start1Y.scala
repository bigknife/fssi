package fssi.scp.ballot5.start1y.steps
import fssi.scp.ballot5.steps.StepSpec
import fssi.scp.types.{Ballot, Value}
import org.scalatest.Matchers._

trait Start1Y extends StepSpec {
  val aValue: Value = yValue
  val bValue: Value = xValue

  val A1: Ballot = Ballot(1, aValue)
  val B1: Ballot = Ballot(1, bValue)

  val A2: Ballot = Ballot(2, aValue)
  val A3: Ballot = Ballot(3, aValue)
  val A4: Ballot = Ballot(4, aValue)
  val A5: Ballot = Ballot(5, aValue)

  val AInf: Ballot = Ballot(Int.MaxValue, aValue)
  val BInf: Ballot = Ballot(Int.MaxValue, bValue)

  val B2: Ballot = Ballot(2, bValue)
  val B3: Ballot = Ballot(3, bValue)

  def start1Y(): Unit = {
    app.shouldNotHaveBallotTimer()

    app.bumpState(aValue) shouldBe true

    app.numberOfEnvelopes shouldBe 1
    app.shouldNotHaveBallotTimer()
  }
}
