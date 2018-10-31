package fssi.scp.nomination
import org.scalatest.Suites

class V0IsTopSpec
    extends Suites(
      new SelfXOthersXSpec(),
      new SelfXOthersYSpec
    ) {
 override def suiteName: String = "v0 is top"
}
