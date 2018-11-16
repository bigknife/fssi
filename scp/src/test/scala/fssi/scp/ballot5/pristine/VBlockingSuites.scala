package fssi.scp.ballot5.pristine
import org.scalatest.Suites

class VBlockingSuites
    extends Suites(
      new VBConfirmSuite(),
      new VBExternalizeSuite()
    ) {}
