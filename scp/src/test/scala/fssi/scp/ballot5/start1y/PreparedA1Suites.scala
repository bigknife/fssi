package fssi.scp.ballot5.start1y
import org.scalatest.Suites

class PreparedA1Suites extends Suites(
  new PreparedA2Suites(),
  new SwitchSuite()
){
  override def suiteName: String = "prepared A1"
}
