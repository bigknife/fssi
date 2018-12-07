package fssi.scp.ballot3.steps

import java.security.PrivateKey

import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
import fssi.scp.interpreter.{LogSupport, NodeServiceHandler, QuorumSetSupport}
import fssi.scp.types.Message.Prepare
import fssi.scp.types.QuorumSet.Slices
import fssi.scp.types._
import fssi.scp.{TestApp, TestSupport, TestValue}
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.collection.immutable.TreeSet

trait TestBed extends FunSuite with TestSupport with BeforeAndAfterEach with LogSupport {
  val (node0, keyOfNode0) = createNodeID()
  val (node1, keyOfNode1) = createNodeID()
  val (node2, keyOfNode2) = createNodeID()

  val slot0: SlotIndex     = SlotIndex(1)

  // core3 has an edge case where v-blocking and quorum can be the same
  // v-blocking set size: 2
  // threshold: 2 = 1 + self or 2 others
  val slice: Slices        = Slices.flat(threshold = 2, node0, node1, node2)
  val quorumSet: QuorumSet = QuorumSet.slices(slice)

  val app: TestApp =
    new TestApp(node0, keyOfNode0, SlotIndex(1), quorumSet, TestValue(TreeSet.empty))

  val xValue: Value = TestValue(TreeSet(1, 2))
  val yValue: Value = TestValue(TreeSet(10, 20))
  val zValue: Value = TestValue(TreeSet(100, 200))

  val votes: Var[ValueSet]    = Var(ValueSet.empty)
  val accepted: Var[ValueSet] = Var(ValueSet.empty)

  override def beforeEach(): Unit = {
    QuorumSetSupport.slicesCache := Map(
      node0 -> slice,
      node1 -> slice,
      node2 -> slice
    )

    xValue < yValue shouldBe true
  }
  override def afterEach(): Unit = {
    QuorumSetSupport.slicesCache := Map.empty

    NominationStatus.clearInstance(slot0)

    BallotStatus.cleanInstance(slot0)

    NodeServiceHandler.instance.resetSlotIndex(node0)
    NodeServiceHandler.instance.resetSlotIndex(node1)
    NodeServiceHandler.instance.resetSlotIndex(node2)

    votes := ValueSet.empty
    accepted := ValueSet.empty

    app.reset()
  }

  def onEnvelopesFromQuorumChecksEx2[M <: Message](
      envGenerator: (NodeID, PrivateKey) => Envelope[M],
      withChecks: Boolean,
      isQuorumDelayed: Boolean,
      checkUpcoming: Boolean,
      minQuorum: Boolean): Unit = {
    val e1: Envelope[M] = envGenerator(node1, keyOfNode1)
    val e2: Envelope[M] = envGenerator(node2, keyOfNode2)

    app.bumpTimerOffset()

    val expected: Int = app.numberOfEnvelopes + 1
    app.onEnvelope(e1)
    if (withChecks && !isQuorumDelayed) {
      app.numberOfEnvelopes shouldBe expected
    }
    if (checkUpcoming) {
      app.shouldBallotTimerUpcoming()
    }
    if (!minQuorum) {
      // nothing happens with an extra vote (unless we're in
      // delayedQuorum)
      app.onEnvelope(e2)
      if (withChecks) {
        app.numberOfEnvelopes shouldBe expected
      }
    }
  }

  def onEnvelopesFromQuorumChecksEx[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M],
                                                  withChecks: Boolean,
                                                  isQuorumDelayed: Boolean,
                                                  checkUpcoming: Boolean): Unit =
    onEnvelopesFromQuorumChecksEx2(gen,
                                   withChecks,
                                   isQuorumDelayed,
                                   checkUpcoming,
                                   minQuorum = false)

  def onEnvelopesFromQuorumChecks[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M],
                                                withChecks: Boolean,
                                                isQuorumDelayed: Boolean): Unit =
    onEnvelopesFromQuorumChecksEx(gen, withChecks, isQuorumDelayed, checkUpcoming = false)

  def onEnvelopesFromQuorumEx[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M],
                                            checkUpcoming: Boolean): Unit =
    onEnvelopesFromQuorumChecksEx(gen, withChecks = true, isQuorumDelayed = false, checkUpcoming)

  def onEnvelopesFromQuorum[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M]): Unit =
    onEnvelopesFromQuorumEx(gen, checkUpcoming = false)

  def makePrepareGen(b: Ballot,
                     p: Option[Ballot] = None,
                     cn: Int = 0,
                     hn: Int = 0,
                     pPrime: Option[Ballot] = None): (NodeID, PrivateKey) => Envelope[Prepare] =
    app.makePrepare(_: NodeID, _: PrivateKey, b, p, cn, hn, pPrime)

  val aValue: Value = yValue
  val bValue: Value = xValue

  val A1: Ballot = Ballot(1, aValue)
  val A2: Ballot = Ballot(2, aValue)
  val A3: Ballot = Ballot(3, aValue)
  val A4: Ballot = Ballot(4, aValue)
  val A5: Ballot = Ballot(5, aValue)

  val B1: Ballot = Ballot(1, bValue)
  val B2: Ballot = Ballot(2, bValue)
  val B3: Ballot = Ballot(3, bValue)

  val AInf: Ballot = Ballot(Int.MaxValue, aValue)
  val BInf: Ballot = Ballot(Int.MaxValue, bValue)
}
