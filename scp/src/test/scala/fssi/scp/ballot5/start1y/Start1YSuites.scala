package fssi.scp.ballot5.start1y
import org.scalatest.Suites

// this is the same test suite than "start <1,x>" with the exception that
// some transitions are not possible as x < y - so instead we verify that
// nothing happens
class Start1YSuites
    extends Suites(
      new PreparedA1Suites(),
      new PrepareBSuite(),
      new ConfirmVBlockingSuite()
      ) {
  override def suiteName: String = "start <1, y>"
}
