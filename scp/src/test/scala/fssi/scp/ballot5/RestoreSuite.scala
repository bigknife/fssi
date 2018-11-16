package fssi.scp.ballot5
import fssi.scp.ballot5.steps.StepSpec
import fssi.scp.types.Ballot
import fssi.scp.{TestApp, TestValue}

import scala.collection.immutable.TreeSet

class RestoreSuite extends StepSpec{
  val scp2: TestApp =new TestApp(node0, keyOfNode0, slot0, quorumSet, TestValue(TreeSet.empty))
  val b: Ballot = Ballot(2, xValue)

  override def suiteName: String = "restore ballot protocol"

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  test("prepare") {
  }

  test("confirm") {}

  test("externalize"){}
}
