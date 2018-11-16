package fssi.scp.nomination
import org.scalatest.Suites

class V0XOthersXSuite
    extends Suites(
      new AcceptYSuite(),
      new RestoreSuite()
    ) {

  override def suiteName: String = "others nominate what v0 says (x) -> prepare x"

}
