package fssi.scp

import fssi.scp.ast.components._
import fssi.scp.ast.uc._
import fssi.scp.interpreter._
import fssi.scp.types._
import org.scalatest._

import scala.collection.immutable._

class SCPSpec extends FunSuite with TestSupport {
  val scp = SCP[Model.Op]

  test("ast") {
    val (nodeId, nodeKey)               = createNodeID()
    val slotIndex            = SlotIndex(1)
    val value: Value         = TestValue(TreeSet(1, 2))
    val previousValue: Value = TestValue(TreeSet(1))

    val setting = Setting(
      quorumSet = QuorumSet.slices(
        QuorumSet.Slices.flat(1, nodeId)
      ),
      localNode = nodeId,
      privateKey = null,
      applicationCallback = ApplicationCallback.unimplemented
    )

    val p = scp.handleAppRequest(nodeId, slotIndex, value, previousValue)
    val r = runner.runIO(p, setting).unsafeRunSync
    info(s"r = $r")
  }
}
