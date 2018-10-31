package fssi.scp.nomination.steps
import fssi.scp.interpreter.{LogSupport, QuorumSetSupport}
import fssi.scp.interpreter.store.NominationStatus
import fssi.scp.types.QuorumSet.Slices
import fssi.scp.types.{NodeID, QuorumSet, SlotIndex, Value}
import fssi.scp.{TestApp, TestSupport, TestValue}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.collection.immutable.TreeSet

trait TestBed extends FunSuite with TestSupport with BeforeAndAfterEach with LogSupport {
  val node0: NodeID = createNodeID()
  val node1: NodeID = createNodeID()
  val node2: NodeID = createNodeID()
  val node3: NodeID = createNodeID()
  val node4: NodeID = createNodeID()

  val slot0: SlotIndex     = SlotIndex(0)
  val slice: Slices        = Slices.flat(4, node0, node1, node2, node3, node4)
  val quorumSet: QuorumSet = QuorumSet.slices(slice)

  val app: TestApp = new TestApp(node0, SlotIndex(0), quorumSet)

  val xValue: Value = TestValue(TreeSet(1, 2))
  val yValue: Value = TestValue(TreeSet(10, 20))
  val zValue: Value = TestValue(TreeSet(100, 200))

  override def beforeEach(): Unit = {
    QuorumSetSupport.slicesCache := Map(node0 -> slice)
    log.debug(s"slices cache[${QuorumSetSupport.slicesCache.unsafe().keys.head}]")
  }
  override def afterEach(): Unit = {
    QuorumSetSupport.slicesCache := Map.empty
    NominationStatus.clearInstance(node0, slot0)
    app.reset()
  }
}
