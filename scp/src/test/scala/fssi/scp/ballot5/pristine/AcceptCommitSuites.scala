package fssi.scp.ballot5.pristine
import org.scalatest.Suites

class AcceptCommitSuites
    extends Suites(
      new QuorumAfterAcceptCommitSuite(),
      new VBlockingSuites()
      ) {
  override def suiteName: String = "Accept commit"
}
