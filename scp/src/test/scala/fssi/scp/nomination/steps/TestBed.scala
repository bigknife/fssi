package fssi.scp.nomination.steps
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
import fssi.scp.interpreter.{LogSupport, NodeServiceHandler, QuorumSetSupport}
import fssi.scp.types.QuorumSet.Slices
import fssi.scp.types._
import fssi.scp.{TestApp, TestSupport, TestValue}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.collection.immutable.TreeSet

trait TestBed extends FunSuite with TestSupport with BeforeAndAfterEach with LogSupport {
  val (node0, keyOfNode0) = createNodeID()
  val (node1, keyOfNode1) = createNodeID()
  val (node2, keyOfNode2) = createNodeID()
  val (node3, keyOfNode3) = createNodeID()
  val (node4, keyOfNode4) = createNodeID()

  val slot0: SlotIndex     = SlotIndex(0)
  val slice: Slices        = Slices.flat(4, node0, node1, node2, node3, node4)
  val quorumSet: QuorumSet = QuorumSet.slices(slice)

  val app: TestApp =
    new TestApp(node0, keyOfNode0, SlotIndex(0), quorumSet, TestValue(TreeSet.empty))

  val xValue: Value = TestValue(TreeSet(1, 2))
  val yValue: Value = TestValue(TreeSet(10, 20))
  val zValue: Value = TestValue(TreeSet(100, 200))

  val votes: Var[ValueSet]    = Var(ValueSet.empty)
  val accepted: Var[ValueSet] = Var(ValueSet.empty)

  val votes2: Var[ValueSet] = Var(ValueSet.empty)

  val slot1: SlotIndex = SlotIndex(1)
  val app2: TestApp    = new TestApp(node0, keyOfNode0, slot1, quorumSet, xValue)

  override def beforeEach(): Unit = {
    QuorumSetSupport.slicesCache := Map(
      node0 -> slice,
      node1 -> slice,
      node2 -> slice,
      node3 -> slice,
      node4 -> slice
    )
  }
  override def afterEach(): Unit = {
    QuorumSetSupport.slicesCache := Map.empty

    NominationStatus.clearInstance(slot0)
    NominationStatus.clearInstance(slot1)

    BallotStatus.cleanInstance(slot0)
    BallotStatus.cleanInstance(slot1)

    NodeServiceHandler.instance.resetSlotIndex(node0)
    NodeServiceHandler.instance.resetSlotIndex(node1)
    NodeServiceHandler.instance.resetSlotIndex(node2)
    NodeServiceHandler.instance.resetSlotIndex(node3)
    NodeServiceHandler.instance.resetSlotIndex(node4)

    votes := ValueSet.empty
    accepted := ValueSet.empty
    votes2 := ValueSet.empty

    app.reset()
  }
}
