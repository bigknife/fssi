package fssi.scp.ballot5.start1x
import org.scalatest.Suites

class ConfirmPreparedA2Suites
    extends Suites(
      new AcceptCommitSuites(),
      new ConflictingPreparedBSuite()
    ) {
  override def suiteName: String = "confirm prepared A2"
}
