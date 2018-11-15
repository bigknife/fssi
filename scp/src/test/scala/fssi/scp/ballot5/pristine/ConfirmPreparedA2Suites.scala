package fssi.scp.ballot5.pristine

import org.scalatest.Suites

class ConfirmPreparedA2Suites
    extends Suites(
      new QuorumAfterA2PreparedSuite(),
      new AcceptCommitSuites()
    ) {
  override def suiteName: String = "confirm prepared A2"
}
