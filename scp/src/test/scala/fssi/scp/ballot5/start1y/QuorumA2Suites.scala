package fssi.scp.ballot5.start1y
import fssi.scp.ballot5.start1y.quorumA2.{QuorumPreparedA3Suites, VBlockingSuite}
import org.scalatest.Suites

class QuorumA2Suites extends Suites(
  new QuorumPreparedA3Suites(),
  new VBlockingSuite()
)
  {
  override def suiteName: String = "Quorum A2"
}
