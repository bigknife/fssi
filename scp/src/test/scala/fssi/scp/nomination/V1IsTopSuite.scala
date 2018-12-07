package fssi.scp.nomination
import org.scalatest.Suites

class V1IsTopSuite extends Suites(
  new WaitForV1Suite(),
  new V1DeadOrTimeoutSuite()
){

  override def suiteName: String = "v1 is top node"

}
