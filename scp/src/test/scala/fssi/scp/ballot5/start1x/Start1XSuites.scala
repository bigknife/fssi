package fssi.scp.ballot5.start1x
import org.scalatest.Suites

class Start1XSuites
    extends Suites(
      new PreparedA1Suites(),
      new PrepareBSuite(),
      new ConfirmVBlockingSuite()
    ) {
  override def suiteId: String = "start <1, x>"
}
