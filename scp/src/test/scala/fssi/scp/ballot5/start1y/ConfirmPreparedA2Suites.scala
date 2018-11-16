package fssi.scp.ballot5.start1y
import org.scalatest.Suites

class ConfirmPreparedA2Suites extends Suites(
  new AcceptCommitSuites(),
  new ConflictingPreparedBSuite()
){
  override def suiteName: String = "Confirm prepared A2"
}
