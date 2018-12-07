package fssi.scp.ballot5.steps

import java.security.PrivateKey

import fssi.scp.interpreter.store.{BallotStatus, NominationStatus, Var}
import fssi.scp.interpreter.{LogSupport, NodeServiceHandler, QuorumSetSupport}
import fssi.scp.types.Message.{Confirm, Externalize, Prepare}
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

  val slot0: SlotIndex     = SlotIndex(1)
  val slice: Slices        = Slices.flat(4, node0, node1, node2, node3, node4)
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
      node2 -> slice,
      node3 -> slice,
      node4 -> slice
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
    NodeServiceHandler.instance.resetSlotIndex(node3)
    NodeServiceHandler.instance.resetSlotIndex(node4)

    votes := ValueSet.empty
    accepted := ValueSet.empty

    app.reset()
  }

  def onEnvelopesFromVBlockingChecks[M <: Message](
      envGenerator: (NodeID, PrivateKey) => Envelope[M],
      withChecks: Boolean): Unit = {
    val e1: Envelope[M] = envGenerator(node1, keyOfNode1)
    val e2: Envelope[M] = envGenerator(node2, keyOfNode2)

    app.bumpTimerOffset()
    val current: Int = app.numberOfEnvelopes

    app.onEnvelope(e1)
    if (withChecks) app.numberOfEnvelopes shouldBe current

    app.onEnvelope(e2)
    if (withChecks) app.numberOfEnvelopes shouldBe (current + 1)
  }

  def onEnvelopesFromVBlocking[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M]): Unit =
    onEnvelopesFromVBlockingChecks(gen, withChecks = true)

  def onEnvelopesFromQuorumChecksEx[M <: Message](envGenerator: (NodeID, PrivateKey) => Envelope[M],
                                                  checkEnvelopes: Boolean,
                                                  isQuorumDelayed: Boolean,
                                                  checkTimers: Boolean): Unit = {
    val e1: Envelope[M] = envGenerator(node1, keyOfNode1)
    val e2: Envelope[M] = envGenerator(node2, keyOfNode2)
    val e3: Envelope[M] = envGenerator(node3, keyOfNode3)
    val e4: Envelope[M] = envGenerator(node4, keyOfNode4)

    app.bumpTimerOffset()

    app.onEnvelope(e1)
    app.onEnvelope(e2)

    val expect: Int = app.numberOfEnvelopes + 1

    app.onEnvelope(e3)
    if (checkEnvelopes && !isQuorumDelayed) {
      app.numberOfEnvelopes shouldBe expect
    }
    if (checkTimers && !isQuorumDelayed) {
      app.shouldBallotTimerUpcoming()
    }
    // nothing happens with an extra vote (unless we're in delayedQuorum)
    app.onEnvelope(e4)
    if (checkEnvelopes && isQuorumDelayed) {
      app.numberOfEnvelopes shouldBe expect
    }
    if (checkTimers && isQuorumDelayed) {
      app.shouldBallotTimerUpcoming()
    }
  }

  def onEnvelopesFromQuorumChecks[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M],
                                                checkEnvelopes: Boolean,
                                                isQuorumDelayed: Boolean): Unit =
    onEnvelopesFromQuorumChecksEx(gen, checkEnvelopes, isQuorumDelayed, checkTimers = false)

  def onEnvelopesFromQuorumEx[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M],
                                            checkTimers: Boolean): Unit =
    onEnvelopesFromQuorumChecksEx(gen, checkEnvelopes = true, isQuorumDelayed = false, checkTimers)

  def onEnvelopesFromQuorum[M <: Message](gen: (NodeID, PrivateKey) => Envelope[M]): Unit =
    onEnvelopesFromQuorumEx(gen, checkTimers = false)

  def nodesAllPledgeToCommit(): Unit = {
    val b: Ballot                           = Ballot(1, xValue)
    val prepare1: Envelope[Message.Prepare] = app.makePrepare(node1, keyOfNode1, b)
    val prepare2: Envelope[Message.Prepare] = app.makePrepare(node2, keyOfNode2, b)
    val prepare3: Envelope[Message.Prepare] = app.makePrepare(node3, keyOfNode3, b)
    val prepare4: Envelope[Message.Prepare] = app.makePrepare(node4, keyOfNode4, b)

    app.bumpState(xValue) shouldBe true
    app.numberOfEnvelopes shouldBe 1
    app.shouldHavePrepared(b)

    app.onEnvelope(prepare1)
    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveHeardNothingFromQuorum()

    app.onEnvelope(prepare2)
    app.numberOfEnvelopes shouldBe 1
    app.shouldHaveHeardNothingFromQuorum()

    app.onEnvelope(prepare3)
    app.numberOfEnvelopes shouldBe 2
    app.shouldHaveHeardFromQuorum(b)

    // We have a quorum including us

    app.shouldHavePrepared(b, Some(b))

    app.onEnvelope(prepare4)
    app.numberOfEnvelopes shouldBe 2

    val prepared1: Envelope[Message.Prepare] = app.makePrepare(node1, keyOfNode1, b, Some(b))
    val prepared2: Envelope[Message.Prepare] = app.makePrepare(node2, keyOfNode2, b, Some(b))
    val prepared3: Envelope[Message.Prepare] = app.makePrepare(node3, keyOfNode3, b, Some(b))
    val prepared4: Envelope[Message.Prepare] = app.makePrepare(node4, keyOfNode4, b, Some(b))

    app.onEnvelope(prepared4)
    app.onEnvelope(prepared3)
    app.numberOfEnvelopes shouldBe 2

    app.onEnvelope(prepared2)
    app.numberOfEnvelopes shouldBe 3

    // confirms prepared
    app.shouldHavePrepared(b, Some(b), b.counter, b.counter)

    // extra statement doesn't do anything
    app.onEnvelope(prepared1)
    app.numberOfEnvelopes shouldBe 3
  }

  def makePrepareGen(b: Ballot,
                     p: Option[Ballot] = None,
                     cn: Int = 0,
                     hn: Int = 0,
                     pPrime: Option[Ballot] = None): (NodeID, PrivateKey) => Envelope[Prepare] =
    app.makePrepare(_: NodeID, _: PrivateKey, b, p, cn, hn, pPrime)

  def makeConfirmGen(pn: Int,
                     b: Ballot,
                     cn: Int,
                     hn: Int): (NodeID, PrivateKey) => Envelope[Confirm] =
    app.makeConfirm(_: NodeID, _: PrivateKey, pn, b, cn, hn)

  def makeExternalizeGen(commit: Ballot, hn: Int): (NodeID, PrivateKey) => Envelope[Externalize] =
    app.makeExternalize(_: NodeID, _: PrivateKey, commit, hn)
}
