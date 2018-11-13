package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.vblocking.{ConfirmSuite, ExternalizeSuite}
import org.scalatest.Suites

class VBlockingSuites
    extends Suites(
      new ConfirmSuite(),
      new ExternalizeSuite()
      ) {
  override def suiteName: String = "v-blocking"
}
