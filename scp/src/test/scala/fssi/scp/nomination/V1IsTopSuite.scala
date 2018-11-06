package fssi.scp.nomination
import org.scalatest.Suites

class V1IsTopSuite extends Suites(
  new WaitForV1Suite()
){

  override def suiteName: String = "v1 is top node"

}
