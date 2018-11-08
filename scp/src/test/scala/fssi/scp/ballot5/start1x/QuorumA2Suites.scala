package fssi.scp.ballot5.start1x
import org.scalatest.Suites

class QuorumA2Suites
    extends Suites(
      new QuorumPreparedA3Suites()
    ) {
  override def suiteName: String = "Quorum A2"
}
