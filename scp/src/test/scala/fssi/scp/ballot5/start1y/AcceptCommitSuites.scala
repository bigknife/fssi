package fssi.scp.ballot5.start1y
import org.scalatest.Suites

class AcceptCommitSuites extends Suites(
  new QuorumA2Suites(),
  new VBlockingSuites()
){
  override def suiteName: String = "Accept commit"
}
