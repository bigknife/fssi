package fssi.scp
import java.security.PrivateKey

import fssi.scp.ast.components.Model
import fssi.scp.ast.components.Model.Op
import fssi.scp.ast.uc.SCP
import fssi.scp.interpreter.store.{BallotStatus, NominationStatus}
import fssi.scp.interpreter.{NodeServiceHandler, Setting, runner}
import fssi.scp.types.Message.Nomination
import fssi.scp.types._
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

  private val started: Timestamp = Timestamp(0l)

  private var dispatchedTimers: Map[String, Runnable] = Map.empty

  val setting: Setting = Setting(
    quorumSet = quorumSet,
    localNode = nodeID,
    privateKey = nodeKey,
    applicationCallback = this
  )
  private val scp: SCP[Op] = SCP[Model.Op]

  def reset(): Unit = {
    statements = Vector.empty

    NominationStatus.clearInstance(slotIndex)
    BallotStatus.cleanInstance(nodeID, slotIndex)

    expectedCandidates = ValueSet.empty
    expectedCompositeValue = None
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

  override def dispatch(timer: String, runnable: Runnable): Unit = {
    dispatchedTimers = dispatchedTimers + (timer -> runnable)
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

  def onEnvelope[M <: Message](envelope: Envelope[M]): Boolean = {
    val p = scp.handleSCPEnvelope(envelope, previousValue)
    runner.runIO(p, setting).unsafeRunSync
  }

  def liftNodePriority(nodeID: NodeID): Unit = {
    nodeWithTopPriority = Some(nodeID)
  }

  def forecastNomination(candidates: ValueSet, compositeValue: Option[Value]): Unit = {
    expectedCandidates = candidates
    expectedCompositeValue = compositeValue
  }

  def hasNominated(voted: ValueSet, accepted: ValueSet): Boolean =
    statements.lastOption map (_.copy(timestamp = started)) contains statementOf(
      Nomination(voted, accepted))

  def numberOfNominations: Int = {
    statements.size
  }

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
