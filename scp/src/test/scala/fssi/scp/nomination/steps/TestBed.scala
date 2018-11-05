package fssi.scp.nomination.steps
import fssi.scp.interpreter.store.Var
import fssi.scp.interpreter.{LogSupport, NodeServiceHandler, QuorumSetSupport, runner}
import fssi.scp.types.Message.Nomination
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

  val votedValues: Var[ValueSet]    = Var(ValueSet.empty)
  val acceptedValues: Var[ValueSet] = Var(ValueSet.empty)

  val anotherVotedValues: Var[ValueSet] = Var(ValueSet.empty)

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
    app.reset()
  }
}
