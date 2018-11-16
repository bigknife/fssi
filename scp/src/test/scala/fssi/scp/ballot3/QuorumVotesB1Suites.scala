package fssi.scp.ballot3
import org.scalatest.Suites

class QuorumVotesB1Suites extends
Suites(
  new QuorumPreparedB1Suite
){
  override def suiteName: String = "quorum votes B1"
}
