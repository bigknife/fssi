package fssi.scp.nomination
import fssi.scp.nomination.steps.V0IsTop
import org.scalatest.FunSuite

class SelfXOthersYSpec extends FunSuite with V0IsTop{
  test("prepare y") {
    v0IsTop()
    info("self nominates 'x', others nominate y -> prepare y")
  }
}
