package fssi.scp.nomination
import fssi.scp.nomination.steps.{
  OthersAcceptYAfterXPrepared,
  OthersNominateWhatV0Nominates,
  V0IsTop
}
import org.scalatest.FunSuite

class AcceptYSuite
    extends FunSuite
    with V0IsTop
    with OthersNominateWhatV0Nominates
    with OthersAcceptYAfterXPrepared {

  override def suiteName: String = "nominate x -> accept x -> prepare (x) ; others accepted y"

  test("update latest to (z=x+y)") {
    v0IsTop()
    othersNominateWhatV0Nominate()
    thenOthersAcceptY()
  }
}
