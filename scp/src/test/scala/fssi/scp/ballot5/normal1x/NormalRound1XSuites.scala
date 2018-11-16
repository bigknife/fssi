package fssi.scp.ballot5.normal1x
import org.scalatest.Suites

class NormalRound1XSuites extends Suites(
  new BumpToBallotSuite()
) {
  override def suiteName: String = "normal round (1,x)"
}
