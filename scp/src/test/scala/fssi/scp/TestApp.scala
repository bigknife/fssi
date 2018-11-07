package fssi.scp
import java.security.PrivateKey

import fssi.scp.TestApp.ScheduledTask
import fssi.scp.ast.components.Model
import fssi.scp.ast.components.Model.Op
import fssi.scp.ast.uc.SCP
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.interpreter.{NodeServiceHandler, NodeStoreHandler, Setting, runner}
import fssi.scp.types.Ballot.Phase
import fssi.scp.types.Message.{Nomination, Prepare}
import fssi.scp.types.{Message, _}
import org.scalameta.logger

class TestApp(nodeID: NodeID,
              nodeKey: PrivateKey,
              slotIndex: SlotIndex,
              quorumSet: QuorumSet,
              previousValue: Value)
    extends interpreter.ApplicationCallback {

  // all statements sent to network
  private var statements: Vector[Statement[_ <: Message]] = Vector.empty

  // expected candidates and predefined composite values, should be set before a nomination step
  private var expectedCandidates: ValueSet          = ValueSet.empty
  private var expectedCompositeValue: Option[Value] = None

  private var nodeWithTopPriority: Option[NodeID] = None

  private var valueHashes: Map[Value, Long] = Map.empty

  private val started: Timestamp = Timestamp(0l)

  private var dispatchedTimers: Map[String, ScheduledTask] = Map.empty
  private var currentTime: Long                            = 0

  private var heardFromQuorums: Vector[Ballot] = Vector.empty

  val setting: Setting = Setting(
    quorumSet = quorumSet,
    localNode = nodeID,
    privateKey = nodeKey,
    applicationCallback = this
  )
  private val scp: SCP[Op] = SCP[Model.Op]

  def reset(): Unit = {
    statements = Vector.empty

    expectedCandidates = ValueSet.empty
    expectedCompositeValue = None

    dispatchedTimers = Map.empty
    currentTime = 0

    heardFromQuorums = Vector.empty
  }

  override def validateValue(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Value.Validity = {
    Value.Validity.FullyValidated
  }

  override def combineValues(nodeId: NodeID,
                             slotIndex: SlotIndex,
                             value: ValueSet): Option[Value] = {
    require(value == expectedCandidates)

    expectedCompositeValue
  }

  override def extractValidValue(nodeId: NodeID,
                                 slotIndex: SlotIndex,
                                 value: Value): Option[Value] = {
    Some(value)
  }

  override def dispatch(timer: String, timeout: Long, runnable: Runnable): Unit = {
    dispatchedTimers = dispatchedTimers + (timer -> ScheduledTask(currentTime + timeout, runnable))
  }

  override def valueConfirmed(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {}

  override def valueExternalized(nodeId: NodeID, slotIndex: SlotIndex, value: Value): Unit = {}

  override def broadcastEnvelope[M <: Message](nodeId: NodeID,
                                               slotIndex: SlotIndex,
                                               envelope: Envelope[M]): Unit = {
    if (isEmittedFromThisNode(envelope)) {
      statements = statements :+ envelope.statement
      logger.debug(s"size of statement in app: ${statements.size}")
    }
  }

  override def isHashFuncProvided: StateChanged = true

  override def hashNodeForPriority(nodeId: NodeID,
                                   slotIndex: SlotIndex,
                                   previousValue: Value,
                                   round: Int): Long =
    if (nodeWithTopPriority exists (_ === nodeId)) 1000l else 0l

  override def hashNodeForNeighbour(nodeId: NodeID,
                                    slotIndex: SlotIndex,
                                    previousValue: Value,
                                    round: Int): Long =
    if (nodeWithTopPriority exists (_ === nodeId)) 1000l else 0l

  override def ballotDidHearFromQuorum(slot: SlotIndex, ballot: Ballot): Unit = {
    if(slot == slotIndex) heardFromQuorums = heardFromQuorums :+ ballot
  }

  def bumpTimeOffset(): Unit = {
    currentTime = currentTime + 5 * 3600
  }

  def hasBallotTimerUpcoming: Boolean = {
    dispatchedTimers.get(BALLOT_TIMER).exists(_.when > currentTime)
  }

  def hasBallotTimer: Boolean = {
    dispatchedTimers.contains(BALLOT_TIMER)
  }

  def hasHeardFromQuorum(b: Ballot): Boolean = heardFromQuorums.lastOption.contains(b)

  def hasHeardNothingFromQuorum: Boolean = heardFromQuorums.isEmpty

  def onEnvelope[M <: Message](envelope: Envelope[M]): Boolean = {
    val p = scp.handleSCPEnvelope(envelope, previousValue)
    runner.runIO(p, setting).unsafeRunSync
  }

  def liftNodePriority(nodeID: NodeID): Unit = {
    nodeWithTopPriority = Some(nodeID)
  }

  def hashOfValue(hash: (Value, Long)*): Unit = {
    valueHashes = valueHashes ++ hash
  }

  def forecastNomination(candidates: ValueSet, compositeValue: Option[Value]): Unit = {
    expectedCandidates = candidates
    expectedCompositeValue = compositeValue
  }

  def hasNominated(voted: ValueSet, accepted: ValueSet): Boolean =
    statements.lastOption map (_.copy(timestamp = started)) contains statementOf(
      Nomination(voted, accepted))

  def numberOfEnvelopes: Int = {
    statements.size
  }

  def latestCompositeCandidateValue: Option[Value] =
    NominationStatus.getInstance(slotIndex).latestCompositeCandidate.unsafe()

  def nominate(value: Value): Boolean = {
    val p = scp.handleAppRequest(nodeID, slotIndex, value, value)
    runner.runIO(p, setting).unsafeRunSync
  }

  def makeNomination(node: NodeID,
                     key: PrivateKey,
                     votedValues: ValueSet,
                     acceptedValues: ValueSet): Envelope[Nomination] = {
    NodeServiceHandler.instance
      .putInEnvelope(
        slotIndex,
        Nomination(votedValues, acceptedValues)
      )
      .run(setting.copy(localNode = node, privateKey = key))
      .unsafeRunSync()
  }

  def makePrepare(node: NodeID,
                  key: PrivateKey,
                  current: Ballot,
                  prepare: Option[Ballot] = None,
                  cn: Int = 0,
                  hn: Int = 0,
                  preparePrime: Option[Ballot] = None): Envelope[Prepare] = {
    NodeServiceHandler.instance
      .putInEnvelope(
        slotIndex,
        Prepare(current, prepare, preparePrime, cn, hn)
      )
      .run(setting.copy(localNode = node, privateKey = key))
      .unsafeRunSync()
  }

  def bumpState(value: Value): Boolean = {
    val p = scp.bumpState(nodeID, slotIndex, value, value, force = true)
    runner.runIO(p, setting).unsafeRunSync()
  }

  def hasPrepared(b: Ballot, p: Option[Ballot] = None, cn: Int = 0, hn: Int = 0, pPrime: Option[Ballot] = None): Boolean =
    statements.lastOption map (_.copy(timestamp = started)) contains statementOf(Prepare(b, p, pPrime, cn, hn))

  def setStatusFromMessage[M <: Message](m: M): Unit = {
    val p = for {
      envelope <- NodeServiceHandler.instance.putInEnvelope(slotIndex, m)
      _        <- NodeStoreHandler.instance.saveEnvelope(nodeID, slotIndex, envelope)
    } yield ()

    p.run(setting).unsafeRunSync()

    m match {
      case Nomination(votedValues, acceptedValues) =>
        val nominationStatus = NominationStatus.getInstance(slotIndex)
        nominationStatus.votes := votedValues
        nominationStatus.accepted := acceptedValues

      case Prepare(b, p, pPrime, cn, hn) =>
        val ballotStatus = BallotStatus.getInstance(nodeID, slotIndex)
        NodeStoreHandler.instance
          .updateBallotStateWhenBumpNewBallot(nodeID, slotIndex, b)
          .run(setting)
          .unsafeRunSync()
        ballotStatus.prepared := p
        ballotStatus.preparedPrime := pPrime
        if (hn > 0) ballotStatus.highBallot := Some(Ballot(hn, b.value))
        if (cn > 0) ballotStatus.commit := Some(Ballot(cn, b.value))
        ballotStatus.phase := Phase.Prepare
    }
  }

  private def isEmittedFromThisNode[M <: Message](envelope: Envelope[M]): Boolean =
    envelope.statement.from == nodeID &&
      envelope.statement.slotIndex == slotIndex && envelope.statement.quorumSet == quorumSet

  private def statementOf[M <: Message](message: M): Statement[M] = Statement[M](
    nodeID,
    slotIndex,
    started,
    quorumSet,
    message
  )
}

object TestApp {
  case class ScheduledTask(when: Long, what: Runnable)
}
