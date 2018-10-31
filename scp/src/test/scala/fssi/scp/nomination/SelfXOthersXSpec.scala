package fssi.scp.nomination
import fssi.scp.nomination.steps.{OthersNominateWhatV0Nominates, V0IsTop}
import org.scalatest.FunSuite

class SelfXOthersXSpec extends FunSuite with V0IsTop with OthersNominateWhatV0Nominates {

  override def suiteName: String = "others nominate what v0 says (x) -> prepare x"

  test("nominate x -> accept x -> prepare (x) ; others accepted y -> update latest to (z=x+y)") {
    v0IsTop()
    othersNominateWhatV0Nominate()
  }
}
