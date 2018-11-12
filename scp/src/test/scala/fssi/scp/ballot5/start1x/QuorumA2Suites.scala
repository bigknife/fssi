package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.quorumA2.{HangToConfirmBSuite, QuorumPreparedA3Suites, VBlockingSuite}
import org.scalatest.Suites

class QuorumA2Suites
    extends Suites(
      new QuorumPreparedA3Suites(),
      new VBlockingSuite(),
      new HangToConfirmBSuite()
    )
    {
  override def suiteName: String = "Quorum A2"


}
