package fssi.scp.ballot5.pristine
import org.scalatest.Suites

class StartFromPristineSuites
    extends Suites(
      new PreparedA1Suites(),
      new PreparedBSuite(),
      new ConfirmVBlockingSuite()
    ) {
  override def suiteName: String = "start from pristine"
}
