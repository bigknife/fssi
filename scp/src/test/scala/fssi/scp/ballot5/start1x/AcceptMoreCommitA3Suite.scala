package fssi.scp.ballot5.start1x
import fssi.scp.ballot5.start1x.steps.AcceptMoreCommitA3
import org.scalatest.FunSuite

class AcceptMoreCommitA3Suite extends FunSuite with AcceptMoreCommitA3{
  override def suiteName: String = "Accept more commit A3"

  override def beforeEach(): Unit = {
    super.beforeEach()

    start1X()
    preparedA1()
  }

  test("Quorum externalize A3") {}
}
