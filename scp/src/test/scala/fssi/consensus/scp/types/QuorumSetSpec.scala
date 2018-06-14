package fssi.consensus.scp.types

import fssi.consensus.scp.ast.domain.types.PublicKey
import org.scalatest.FunSuite
import fssi.crypto._
import fssi.consensus.scp.ast.domain.types._
import fssi.consensus.scp.ast.domain.types.op._

class QuorumSetSpec extends FunSuite {
  private def isNear(v: Long, target: Double): Boolean = {
    ((v.toDouble / Long.MaxValue) - target).abs < 0.01
  }

  test("Quorum set calculate node weight") {
    val nodeNames = Vector("node-1", "node-2", "node-3", "node-4", "node-5", "node-6")
    val nodeIds = nodeNames.map(x => crypto.sha3(x.getBytes))

    // only 4 validators, threshold = 3
    def case1(): Unit = {
      val qs0 = QuorumSet.simple(
        threshold = 3,
        validators = nodeIds.take(4).map(PublicKey)
      )
      val weight0 = qs0.nodeWeight(Node.ID(nodeIds(0)))
      assert(isNear(weight0, 0.75))

      val weigh1 = qs0.nodeWeight(Node.ID(nodeIds(4)))
      assert(weigh1 == 0)
      ()
    }

    case1()

    def case2(): Unit = {
      val qs0 = QuorumSet.simple(threshold = 1, validators = nodeIds.drop(4).map(PublicKey))
      val qs1 = QuorumSet.twoLayers(threshold = 3, validators = nodeIds.take(4).map(PublicKey), Vector(qs0))
      val weight0 = qs1.nodeWeight(Node.ID(nodeIds(5)))
      assert(isNear(weight0, 0.5 * 0.6))
      ()
    }

    case2()
  }

  test("Quorum set for all") {
    val nodeNames = Vector("node-1", "node-2", "node-3", "node-4", "node-5", "node-1")
    val qs = QuorumSet.simple(4, nodeNames.map(x => crypto.sha3(x.getBytes())).map(PublicKey))
    val counter: collection.mutable.ListBuffer[Int] = collection.mutable.ListBuffer.empty
    qs.forAll {nodeId =>
      info(nodeId.bytes.map("%02x" format _).mkString(""))
      counter.append(1)
    }

    assert(counter.size == 5)
  }

}