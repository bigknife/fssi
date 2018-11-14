package fssi.scp.ballot5.start1y
import org.scalatest.Suites

class Start1YSuites
    extends Suites(
      new PreparedA1Suites(),
      new PrepareBSuite(),
      new ConfirmVBlockingSuite()
      ) {
  override def suiteName: String = "start <1, y>"
}
