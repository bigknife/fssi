package fssi.scp

import org.scalatest._
import ast.uc._
import ast.components._
import fssi.scp.types.QuorumSet.QuorumSlices
import interpreter._
import types._
import fssi.utils._

import scala.collection.immutable._

class SCPSpec extends FunSuite with TestSupport {
  val scp = SCP[Model.Op]


  test("hashValue to uint64") {
    val c = new CryptoSupport {}
    val h = c.computeHashValue("Hello,world. Fox".getBytes())
    info(s"${h.signum}: $h, ${h.toLong}")
  }

  test("ast") {
    val nodeId = createNodeID()
    val slotIndex = SlotIndex(1)
    val value: Value = TestValue(TreeSet(1,2))
    val previousValue: Value = TestValue(TreeSet(1))

    val setting = Setting(
      quorumSet = QuorumSet.slices(
        QuorumSet.Slices.flat(1, nodeId)
      ),
      privateKey = null,
      applicationCallback = ApplicationCallback.unimplemented
    )

    val p = scp.handleAppRequest(nodeId, slotIndex, value, previousValue)
    val r = runner.runIO(p, setting).unsafeRunSync
    info(s"r = $r")
  }
}
