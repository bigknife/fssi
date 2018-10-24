package fssi.scp

import org.scalatest._
import ast.uc._
import ast.components._
import interpreter._
import types._
import fssi.utils._
import scala.collection.immutable._

class SCPSpec extends FunSuite with TestSupport {
  val scp = SCP[Model.Op]
  val setting = Setting()

  test("ast") {
    val nodeId = createNodeID()
    val slotIndex = SlotIndex(1)
    val value: Value = TestValue(TreeSet(1,2))
    val previousValue: Value = TestValue(TreeSet(1))

    val p = scp.handleAppRequest(nodeId, slotIndex, value, previousValue)
    runner.runIOAttempt(p, setting).unsafeRunSync
  }
}
