package fssi.scp.ballot5.start1y
import org.scalatest.Suites

class PreparedA2Suites extends Suites(
  new ConfirmPreparedA2Suites(),
  new ConfirmPreparedMixedSuite()
){
  override def suiteName: String = "bump prepared A2"
}
