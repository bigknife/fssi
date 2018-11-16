package fssi.scp.nomination
import org.scalatest.Suites

class V0IsTopSuite
    extends Suites(
      new V0XOthersXSuite(),
      new V0XOthersYSuite()
    ) {
 override def suiteName: String = "v0 is top"
}
