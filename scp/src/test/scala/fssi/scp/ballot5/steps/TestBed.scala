package fssi.scp.ballot5.steps

import java.security.PrivateKey

import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
import fssi.scp.interpreter.{LogSupport, NodeServiceHandler, QuorumSetSupport}
import fssi.scp.types.QuorumSet.Slices
import fssi.scp.types._
import fssi.scp.{TestApp, TestSupport, TestValue}
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.collection.immutable.TreeSet
import org.scalatest.Matchers._

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

  override def beforeEach(): Unit = {
    QuorumSetSupport.slicesCache := Map(
      node0 -> slice,
      node1 -> slice,
      node2 -> slice,
      node3 -> slice,
      node4 -> slice
    )

    xValue < yValue shouldBe true
  }
  override def afterEach(): Unit = {
    QuorumSetSupport.slicesCache := Map.empty

    NominationStatus.clearInstance(slot0)

    BallotStatus.cleanInstance(node0, slot0)
    BallotStatus.cleanInstance(node1, slot0)
    BallotStatus.cleanInstance(node2, slot0)
    BallotStatus.cleanInstance(node3, slot0)
    BallotStatus.cleanInstance(node4, slot0)

    NodeServiceHandler.instance.resetSlotIndex(node0)
    NodeServiceHandler.instance.resetSlotIndex(node1)
    NodeServiceHandler.instance.resetSlotIndex(node2)
    NodeServiceHandler.instance.resetSlotIndex(node3)
    NodeServiceHandler.instance.resetSlotIndex(node4)

    votes := ValueSet.empty
    accepted := ValueSet.empty

    app.reset()
  }

  def onEnvelopesFromVBlockingWithChecks[M <: Message](gen: PrivateKey => Envelope[M],
                                                       withChecks: Boolean): Unit = {
    val e1: Envelope[M] = gen(keyOfNode1)
    val e2: Envelope[M] = gen(keyOfNode2)

    app.bumpTimeOffset()
    val current: Int = app.numberOfEnvelopes

    app.onEnvelope(e1)
    if (withChecks) app.numberOfEnvelopes shouldBe current

    app.onEnvelope(e2)
    if (withChecks) app.numberOfEnvelopes shouldBe (current + 1)
  }

  def onEnvelopesFromVBlocking[M <: Message](gen: PrivateKey => Envelope[M]): Unit =
    onEnvelopesFromVBlockingWithChecks(gen, withChecks = true)

  def onEnvelopesFromQuorumWithChecksEx[M <: Message](gen: PrivateKey => Envelope[M],
                                                    checkEnvelopes: Boolean,
                                                    isQuorumDelayed: Boolean,
                                                    checkTimers: Boolean): Unit = {
    val e1: Envelope[M] = gen(keyOfNode1)
    val e2: Envelope[M] = gen(keyOfNode2)
    val e3: Envelope[M] = gen(keyOfNode1)
    val e4: Envelope[M] = gen(keyOfNode2)

    app.bumpTimeOffset()

    app.onEnvelope(e1)
    app.onEnvelope(e2)

    val expect: Int = app.numberOfEnvelopes + 1

    app.onEnvelope(e3)
    if (checkEnvelopes && !isQuorumDelayed) {
      app.numberOfEnvelopes shouldBe expect
    }
    if (checkTimers && !isQuorumDelayed) {
      app.hasBallotTimerUpcoming shouldBe true
    }
    // nothing happens with an extra vote (unless we're in delayedQuorum)
    app.onEnvelope(e4)
    if (checkEnvelopes && isQuorumDelayed) {
      app.numberOfEnvelopes shouldBe expect
    }
    if (checkTimers && isQuorumDelayed) {
      app.hasBallotTimerUpcoming shouldBe true
    }
  }


  def onEnvelopesFromQuorumWithChecks[M <: Message](gen: PrivateKey => Envelope[M],
                                                    checkEnvelopes: Boolean,
                                                    isQuorumDelayed: Boolean): Unit = onEnvelopesFromQuorumWithChecksEx(gen, checkEnvelopes, isQuorumDelayed, checkTimers = false)

  def onEnvelopesFromQuorumWithEx[M <: Message](gen: PrivateKey => Envelope[M],
                                                checkTimers: Boolean): Unit = onEnvelopesFromQuorumWithChecksEx(gen, checkEnvelopes = true, isQuorumDelayed = false, checkTimers)

  def onEnvelopesFromQuorum[M <: Message](gen: PrivateKey => Envelope[M]): Unit = onEnvelopesFromQuorumWithEx(gen, checkTimers = false)

  def nodesAllPledgeToCommit(): Unit = {
    val b: Ballot = Ballot(1, xValue)
    val prepare1: Envelope[Message.Prepare] = app.makePrepare(node1, keyOfNode1, b )
    val prepare2: Envelope[Message.Prepare] = app.makePrepare(node2, keyOfNode2, b )
    val prepare3: Envelope[Message.Prepare] = app.makePrepare(node3, keyOfNode3, b )
    val prepare4: Envelope[Message.Prepare] = app.makePrepare(node4, keyOfNode4, b )

    app.bumpState(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1
    app.hasPrepared(b)

    app.onEnvelope(prepare1)
    app.numberOfEnvelopes shouldBe 1
    app.hasHeardNothingFromQuorum shouldBe true

    app.onEnvelope(prepare2)
    app.numberOfEnvelopes shouldBe 1
    app.hasHeardNothingFromQuorum shouldBe true

    app.onEnvelope(prepare3)
    app.numberOfEnvelopes shouldBe 2
    app.hasHeardFromQuorum(b) shouldBe true

    // We have a quorum including us

    app.hasPrepared(b, Some(b))

    app.onEnvelope(prepare4)
    app.numberOfEnvelopes shouldBe 2

    val prepared1: Envelope[Message.Prepare] = app.makePrepare(node1, keyOfNode1, b, Some(b))
    val prepared2: Envelope[Message.Prepare] = app.makePrepare(node2, keyOfNode2, b, Some(b) )
    val prepared3: Envelope[Message.Prepare] = app.makePrepare(node3, keyOfNode3, b, Some(b) )
    val prepared4: Envelope[Message.Prepare] = app.makePrepare(node4, keyOfNode4, b, Some(b) )

    app.onEnvelope(prepared4)
    app.onEnvelope(prepared3)
    app.numberOfEnvelopes shouldBe 2

    app.onEnvelope(prepared2)
    app.numberOfEnvelopes shouldBe 3

    // confirms prepared
    app.hasPrepared(b, Some(b), b.counter, b.counter)

    // extra statement doesn't do anything
    app.onEnvelope(prepared1)
    app.numberOfEnvelopes shouldBe 3
  }
}
