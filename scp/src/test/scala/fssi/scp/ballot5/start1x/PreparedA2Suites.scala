package fssi.scp.ballot5.start1x
import org.scalatest.Suites

class PreparedA2Suites
    extends Suites(
      new ConfirmPreparedA2Suites()
    ) {
  override def suiteName: String = "bump prepared A2"
}
